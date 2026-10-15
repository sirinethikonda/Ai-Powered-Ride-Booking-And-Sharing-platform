package com.rideapp.dispatch.service;

import com.rideapp.booking.entity.Ride;
import com.rideapp.booking.entity.RideStatus;
import com.rideapp.booking.repository.RideRepository;
import com.rideapp.common.exception.BusinessRuleException;
import com.rideapp.common.exception.ConflictException;
import com.rideapp.common.exception.InvalidStateTransitionException;
import com.rideapp.common.exception.ResourceNotFoundException;
import com.rideapp.dispatch.dto.OfferResponse;
import com.rideapp.dispatch.entity.AssignmentStatus;
import com.rideapp.dispatch.entity.DriverOffer;
import com.rideapp.dispatch.entity.OfferStatus;
import com.rideapp.dispatch.entity.RideAssignment;
import com.rideapp.dispatch.repository.DriverOfferRepository;
import com.rideapp.dispatch.repository.RideAssignmentRepository;
import com.rideapp.driver.entity.Driver;
import com.rideapp.driver.entity.OperationalStatus;
import com.rideapp.driver.entity.Vehicle;
import com.rideapp.driver.repository.DriverRepository;
import com.rideapp.driver.repository.VehicleRepository;
import com.rideapp.location.dto.NearbyDriverDto;
import com.rideapp.location.service.DriverLocationService;
import com.rideapp.messaging.event.DriverAssignedEvent;
import com.rideapp.messaging.event.DriverOfferCreatedEvent;
import com.rideapp.messaging.event.DriverOfferRejectedEvent;
import com.rideapp.messaging.event.DriverReplacementRequestedEvent;
import com.rideapp.messaging.outbox.OutboxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DriverDispatchService {

    private static final Logger log = LoggerFactory.getLogger(DriverDispatchService.class);

    private final RideRepository rideRepository;
    private final DriverOfferRepository driverOfferRepository;
    private final RideAssignmentRepository rideAssignmentRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverLocationService driverLocationService;
    private final OutboxService outboxService;

    private final int offerTimeoutSeconds;
    private final int maxReplacementAttempts;
    private final double searchRadiusKm;

    public DriverDispatchService(
            RideRepository rideRepository,
            DriverOfferRepository driverOfferRepository,
            RideAssignmentRepository rideAssignmentRepository,
            DriverRepository driverRepository,
            VehicleRepository vehicleRepository,
            DriverLocationService driverLocationService,
            OutboxService outboxService,
            @Value("${app.dispatch.offer-timeout-seconds:30}") int offerTimeoutSeconds,
            @Value("${app.dispatch.max-replacement-attempts:3}") int maxReplacementAttempts,
            @Value("${app.dispatch.search-radius-km:5.0}") double searchRadiusKm) {
        this.rideRepository = rideRepository;
        this.driverOfferRepository = driverOfferRepository;
        this.rideAssignmentRepository = rideAssignmentRepository;
        this.driverRepository = driverRepository;
        this.vehicleRepository = vehicleRepository;
        this.driverLocationService = driverLocationService;
        this.outboxService = outboxService;
        this.offerTimeoutSeconds = offerTimeoutSeconds;
        this.maxReplacementAttempts = maxReplacementAttempts;
        this.searchRadiusKm = searchRadiusKm;
    }

    @Transactional
    public Optional<OfferResponse> dispatchRide(UUID rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride", rideId));

        if (ride.getStatus() == RideStatus.REQUESTED) {
            ride.transitionTo(RideStatus.SEARCHING_FOR_DRIVER);
            rideRepository.save(ride);
        }

        List<UUID> excludedDriverIds = driverOfferRepository.findDriverIdsAlreadyOfferedForRide(rideId);

        List<NearbyDriverDto> nearbyCandidates = driverLocationService.findNearbyAvailableDrivers(
                ride.getPickupGeom().getX(),
                ride.getPickupGeom().getY(),
                searchRadiusKm,
                20
        );

        Optional<NearbyDriverDto> selectedCandidate = nearbyCandidates.stream()
                .filter(c -> !excludedDriverIds.contains(c.getDriverId()))
                .findFirst();

        if (selectedCandidate.isEmpty()) {
            log.warn("No available replacement drivers found within {}km for ride {}", searchRadiusKm, rideId);
            return Optional.empty();
        }

        UUID driverId = selectedCandidate.get().getDriverId();
        Driver driver = driverRepository.findById(driverId).orElseThrow();

        BigDecimal fare = ride.getFareBreakdown() != null ? ride.getFareBreakdown().getFinalFare() : BigDecimal.valueOf(100);
        BigDecimal earnings = fare.multiply(new BigDecimal("0.80")).setScale(2, RoundingMode.HALF_UP);

        Instant expiresAt = Instant.now().plusSeconds(offerTimeoutSeconds);
        DriverOffer offer = new DriverOffer(ride, driver, 1, earnings, expiresAt);
        offer = driverOfferRepository.save(offer);

        outboxService.recordEvent(
                "DriverOffer",
                offer.getId().toString(),
                "DriverOfferCreatedEvent",
                "driver.offers",
                driverId.toString(),
                DriverOfferCreatedEvent.of(offer.getId(), rideId, driverId, earnings, expiresAt)
        );

        return Optional.of(OfferResponse.fromEntity(offer));
    }

    @Transactional
    public OfferResponse acceptOffer(UUID offerId, UUID driverId) {
        DriverOffer offer = driverOfferRepository.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("DriverOffer", offerId));

        if (!offer.getDriver().getId().equals(driverId)) {
            throw new BusinessRuleException("Authenticated driver is not the recipient of this offer");
        }

        if (offer.getStatus() != OfferStatus.OFFERED) {
            throw new InvalidStateTransitionException(
                    String.format("Offer is not in OFFERED state (current: %s)", offer.getStatus())
            );
        }

        // Validate offer expiry deadline
        if (offer.isExpired()) {
            offer.setStatus(OfferStatus.EXPIRED);
            offer.setRespondedAt(Instant.now());
            driverOfferRepository.save(offer);

            // Automatically trigger replacement workflow on timeout
            triggerDriverReplacement(offer.getRide().getId(), driverId, "Offer expired before acceptance", offer.getAttemptNumber());
            throw new InvalidStateTransitionException("Offer has expired and is no longer available");
        }

        Ride ride = offer.getRide();

        // Concurrency Guard: Atomic check to ensure no active assignment exists
        Optional<RideAssignment> existingActive = rideAssignmentRepository.findByRideIdAndStatus(ride.getId(), AssignmentStatus.ACTIVE);
        if (existingActive.isPresent()) {
            offer.setStatus(OfferStatus.CANCELLED);
            driverOfferRepository.save(offer);
            throw new ConflictException("Ride has already been claimed by another driver");
        }

        offer.setStatus(OfferStatus.ACCEPTED);
        offer.setRespondedAt(Instant.now());
        driverOfferRepository.save(offer);

        List<Vehicle> vehicles = vehicleRepository.findByDriverIdAndIsActiveTrue(driverId);
        Vehicle vehicle = vehicles.isEmpty() ? null : vehicles.get(0);

        RideAssignment assignment = new RideAssignment(ride, offer.getDriver(), vehicle);
        rideAssignmentRepository.save(assignment);

        ride.setDriver(offer.getDriver());
        ride.transitionTo(RideStatus.DRIVER_ASSIGNED);
        rideRepository.save(ride);

        // Remove driver from Redis available pool
        driverLocationService.removeDriverFromGeo(driverId);

        // Cancel any remaining open offers for this ride
        List<DriverOffer> otherOffers = driverOfferRepository.findByRideId(ride.getId());
        for (DriverOffer other : otherOffers) {
            if (other.getStatus() == OfferStatus.OFFERED && !other.getId().equals(offerId)) {
                other.setStatus(OfferStatus.CANCELLED);
                driverOfferRepository.save(other);
            }
        }

        outboxService.recordEvent(
                "Ride",
                ride.getId().toString(),
                "DriverAssignedEvent",
                "ride.events",
                ride.getId().toString(),
                DriverAssignedEvent.of(ride.getId(), driverId, vehicle != null ? vehicle.getId() : null)
        );

        return OfferResponse.fromEntity(offer);
    }

    @Transactional
    public OfferResponse rejectOffer(UUID offerId, UUID driverId, String reason) {
        DriverOffer offer = driverOfferRepository.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("DriverOffer", offerId));

        if (!offer.getDriver().getId().equals(driverId)) {
            throw new BusinessRuleException("Authenticated driver is not the recipient of this offer");
        }

        if (offer.getStatus() != OfferStatus.OFFERED) {
            throw new InvalidStateTransitionException("Offer is not in OFFERED state");
        }

        offer.setStatus(OfferStatus.REJECTED);
        offer.setRespondedAt(Instant.now());
        driverOfferRepository.save(offer);

        outboxService.recordEvent(
                "DriverOffer",
                offer.getId().toString(),
                "DriverOfferRejectedEvent",
                "driver.events",
                driverId.toString(),
                DriverOfferRejectedEvent.of(offerId, offer.getRide().getId(), driverId, reason)
        );

        // Trigger intelligent replacement workflow
        triggerDriverReplacement(offer.getRide().getId(), driverId, reason != null ? reason : "Driver rejected offer", offer.getAttemptNumber());

        return OfferResponse.fromEntity(offer);
    }

    @Transactional
    public void triggerDriverReplacement(UUID rideId, UUID failedDriverId, String reason, int currentAttempt) {
        Ride ride = rideRepository.findById(rideId).orElse(null);
        if (ride == null || ride.getStatus().isTerminal() || ride.getStatus() == RideStatus.IN_PROGRESS) {
            log.info("Replacement aborted: Ride {} is either missing or in non-replaceable state ({})",
                    rideId, ride != null ? ride.getStatus() : "null");
            return;
        }

        if (currentAttempt >= maxReplacementAttempts) {
            log.warn("Exhausted max replacement attempts ({}) for ride {}. Marking ride as FAILED.",
                    maxReplacementAttempts, rideId);
            ride.setStatus(RideStatus.FAILED);
            rideRepository.save(ride);
            return;
        }

        // Close previous assignment if active
        rideAssignmentRepository.findByRideIdAndStatus(rideId, AssignmentStatus.ACTIVE).ifPresent(assignment -> {
            assignment.setStatus(AssignmentStatus.REPLACED);
            assignment.setReplacementReason(reason);
            assignment.setUnassignedAt(Instant.now());
            rideAssignmentRepository.save(assignment);
        });

        ride.setDriver(null);
        ride.setStatus(RideStatus.SEARCHING_FOR_DRIVER);
        rideRepository.save(ride);

        List<UUID> excludedDriverIds = driverOfferRepository.findDriverIdsAlreadyOfferedForRide(rideId);
        if (failedDriverId != null && !excludedDriverIds.contains(failedDriverId)) {
            excludedDriverIds.add(failedDriverId);
        }

        List<NearbyDriverDto> nearbyCandidates = driverLocationService.findNearbyAvailableDrivers(
                ride.getPickupGeom().getX(),
                ride.getPickupGeom().getY(),
                searchRadiusKm,
                20
        );

        Optional<NearbyDriverDto> nextCandidate = nearbyCandidates.stream()
                .filter(c -> !excludedDriverIds.contains(c.getDriverId()))
                .findFirst();

        if (nextCandidate.isEmpty()) {
            log.warn("No replacement candidates available for ride {}. Retrying within bounds or failing.", rideId);
            if (currentAttempt + 1 >= maxReplacementAttempts) {
                ride.setStatus(RideStatus.FAILED);
                rideRepository.save(ride);
            }
            return;
        }

        UUID nextDriverId = nextCandidate.get().getDriverId();
        Driver nextDriver = driverRepository.findById(nextDriverId).orElseThrow();

        BigDecimal fare = ride.getFareBreakdown() != null ? ride.getFareBreakdown().getFinalFare() : BigDecimal.valueOf(100);
        BigDecimal earnings = fare.multiply(new BigDecimal("0.80")).setScale(2, RoundingMode.HALF_UP);

        Instant expiresAt = Instant.now().plusSeconds(offerTimeoutSeconds);
        DriverOffer newOffer = new DriverOffer(ride, nextDriver, currentAttempt + 1, earnings, expiresAt);
        newOffer = driverOfferRepository.save(newOffer);

        outboxService.recordEvent(
                "Ride",
                rideId.toString(),
                "DriverReplacementRequestedEvent",
                "driver.replacements",
                rideId.toString(),
                DriverReplacementRequestedEvent.of(rideId, failedDriverId, reason, currentAttempt + 1)
        );

        outboxService.recordEvent(
                "DriverOffer",
                newOffer.getId().toString(),
                "DriverOfferCreatedEvent",
                "driver.offers",
                nextDriverId.toString(),
                DriverOfferCreatedEvent.of(newOffer.getId(), rideId, nextDriverId, earnings, expiresAt)
        );

        log.info("Replacement offer {} created for driver {} (attempt {}) on ride {}",
                newOffer.getId(), nextDriverId, currentAttempt + 1, rideId);
    }
}
