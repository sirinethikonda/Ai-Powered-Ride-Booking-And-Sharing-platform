package com.rideapp.sharedride.service;

import com.rideapp.booking.entity.Ride;
import com.rideapp.booking.entity.RideType;
import com.rideapp.booking.repository.RideRepository;
import com.rideapp.common.exception.BusinessRuleException;
import com.rideapp.common.exception.ResourceNotFoundException;
import com.rideapp.messaging.event.SharedRideMatchConfirmedEvent;
import com.rideapp.messaging.outbox.OutboxService;
import com.rideapp.pricing.entity.FareBreakdown;
import com.rideapp.pricing.repository.FareBreakdownRepository;
import com.rideapp.pricing.service.PricingService;
import com.rideapp.sharedride.entity.GenderMatchingPolicy;
import com.rideapp.sharedride.entity.RidePassenger;
import com.rideapp.sharedride.entity.SharedGroupStatus;
import com.rideapp.sharedride.entity.SharedRideGroup;
import com.rideapp.sharedride.repository.RidePassengerRepository;
import com.rideapp.sharedride.repository.SharedRideGroupRepository;
import com.rideapp.user.entity.Gender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SharedRideService {

    private static final Logger log = LoggerFactory.getLogger(SharedRideService.class);

    private final RideRepository rideRepository;
    private final SharedRideGroupRepository sharedRideGroupRepository;
    private final RidePassengerRepository ridePassengerRepository;
    private final FareBreakdownRepository fareBreakdownRepository;
    private final PricingService pricingService;
    private final OutboxService outboxService;

    private final BigDecimal discountFactor;
    private final double maxPickupDistanceKm = 2.5;
    private final double maxDropoffDistanceKm = 3.5;

    public SharedRideService(
            RideRepository rideRepository,
            SharedRideGroupRepository sharedRideGroupRepository,
            RidePassengerRepository ridePassengerRepository,
            FareBreakdownRepository fareBreakdownRepository,
            PricingService pricingService,
            OutboxService outboxService,
            @Value("${app.shared-ride.shared-discount-factor:0.70}") double discountFactor) {
        this.rideRepository = rideRepository;
        this.sharedRideGroupRepository = sharedRideGroupRepository;
        this.ridePassengerRepository = ridePassengerRepository;
        this.fareBreakdownRepository = fareBreakdownRepository;
        this.pricingService = pricingService;
        this.outboxService = outboxService;
        this.discountFactor = BigDecimal.valueOf(discountFactor);
    }

    @Transactional
    public SharedRideGroup matchOrEnqueueSharedRide(UUID rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride", rideId));

        if (ride.getRideType() != RideType.SHARED) {
            throw new BusinessRuleException("Cannot execute shared ride matching on a SOLO ride");
        }

        Gender passengerGender = ride.getPassenger().getGender();

        // Query open shared groups matching the passenger's gender
        List<SharedRideGroup> openGroups = sharedRideGroupRepository.findByStatusAndAllowedGender(
                SharedGroupStatus.OPEN, passengerGender
        );

        Optional<SharedRideGroup> compatibleGroup = openGroups.stream()
                .filter(group -> isRouteCompatible(group, ride))
                .findFirst();

        if (compatibleGroup.isPresent()) {
            return joinSharedGroup(compatibleGroup.get(), ride);
        } else {
            return createNewSharedGroup(ride);
        }
    }

    private boolean isRouteCompatible(SharedRideGroup group, Ride candidateRide) {
        List<RidePassenger> existingPassengers = ridePassengerRepository.findBySharedGroupId(group.getId());
        if (existingPassengers.isEmpty()) {
            return true;
        }

        // Check against the primary passenger in the group
        Ride primaryRide = existingPassengers.get(0).getRide();

        // 1. Strict Gender Compatibility Verification
        Gender genderA = primaryRide.getPassenger().getGender();
        Gender genderB = candidateRide.getPassenger().getGender();
        if (!genderA.isStrictlyCompatibleWith(genderB)) {
            log.info("Shared match rejected: Gender incompatible ({} vs {})", genderA, genderB);
            return false;
        }

        // 2. Pickup Proximity Check
        double pickupDist = pricingService.calculateHaversineDistance(
                primaryRide.getPickupGeom().getY(), primaryRide.getPickupGeom().getX(),
                candidateRide.getPickupGeom().getY(), candidateRide.getPickupGeom().getX()
        );
        if (pickupDist > maxPickupDistanceKm) {
            return false;
        }

        // 3. Dropoff Proximity Check
        double dropoffDist = pricingService.calculateHaversineDistance(
                primaryRide.getDropoffGeom().getY(), primaryRide.getDropoffGeom().getX(),
                candidateRide.getDropoffGeom().getY(), candidateRide.getDropoffGeom().getX()
        );
        return dropoffDist <= maxDropoffDistanceKm;
    }

    private SharedRideGroup joinSharedGroup(SharedRideGroup group, Ride secondaryRide) {
        List<RidePassenger> existingPassengers = ridePassengerRepository.findBySharedGroupId(group.getId());
        Ride primaryRide = existingPassengers.get(0).getRide();

        // Apply conditional discounts to both passengers independently
        FareBreakdown fareA = primaryRide.getFareBreakdown();
        FareBreakdown fareB = secondaryRide.getFareBreakdown();

        BigDecimal sharedFareA = fareA.getBaseFare().add(fareA.getDistanceFare()).add(fareA.getTimeFare())
                .multiply(discountFactor).setScale(2, RoundingMode.HALF_UP);
        BigDecimal discountA = fareA.getFinalFare().subtract(sharedFareA);
        fareA.setSharedDiscountApplied(discountA);
        fareA.setFinalFare(sharedFareA);
        fareBreakdownRepository.save(fareA);

        BigDecimal sharedFareB = fareB.getBaseFare().add(fareB.getDistanceFare()).add(fareB.getTimeFare())
                .multiply(discountFactor).setScale(2, RoundingMode.HALF_UP);
        BigDecimal discountB = fareB.getFinalFare().subtract(sharedFareB);
        fareB.setSharedDiscountApplied(discountB);
        fareB.setFinalFare(sharedFareB);
        fareBreakdownRepository.save(fareB);

        // Record secondary passenger in the shared group
        RidePassenger passengerB = new RidePassenger(
                group,
                secondaryRide,
                secondaryRide.getPassenger(),
                1,
                2,
                2,
                sharedFareB
        );
        ridePassengerRepository.save(passengerB);

        secondaryRide.setSharedGroupId(group.getId());
        rideRepository.save(secondaryRide);

        group.setStatus(SharedGroupStatus.MATCHED);
        group = sharedRideGroupRepository.save(group);

        // Publish SharedRideMatchConfirmedEvent
        outboxService.recordEvent(
                "SharedRideGroup",
                group.getId().toString(),
                "SharedRideMatchConfirmedEvent",
                "shared-ride.events",
                group.getId().toString(),
                SharedRideMatchConfirmedEvent.of(
                        group.getId(),
                        primaryRide.getId(),
                        secondaryRide.getId(),
                        sharedFareA,
                        sharedFareB
                )
        );

        log.info("Shared ride match confirmed for Group {}: Passenger A ({}) pays ₹{}, Passenger B ({}) pays ₹{}",
                group.getId(), primaryRide.getPassenger().getId(), sharedFareA, secondaryRide.getPassenger().getId(), sharedFareB);

        return group;
    }

    private SharedRideGroup createNewSharedGroup(Ride primaryRide) {
        SharedRideGroup group = new SharedRideGroup(
                primaryRide.getPassenger().getGender(),
                GenderMatchingPolicy.STRICT_GENDER_COMPATIBLE,
                BigDecimal.valueOf(30.00)
        );
        group = sharedRideGroupRepository.save(group);

        BigDecimal currentFare = primaryRide.getFareBreakdown() != null ? primaryRide.getFareBreakdown().getFinalFare() : BigDecimal.valueOf(100);

        RidePassenger passengerA = new RidePassenger(
                group,
                primaryRide,
                primaryRide.getPassenger(),
                1,
                1,
                1,
                currentFare
        );
        ridePassengerRepository.save(passengerA);

        primaryRide.setSharedGroupId(group.getId());
        rideRepository.save(primaryRide);

        log.info("Created new open shared ride group {} for gender {}", group.getId(), primaryRide.getPassenger().getGender());
        return group;
    }

    @Transactional(readOnly = true)
    public SharedRideGroup getGroupDetails(UUID groupId) {
        return sharedRideGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("SharedRideGroup", groupId));
    }
}
