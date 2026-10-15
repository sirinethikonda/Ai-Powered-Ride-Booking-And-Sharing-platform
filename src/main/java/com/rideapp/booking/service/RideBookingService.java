package com.rideapp.booking.service;

import com.rideapp.booking.dto.CreateRideRequest;
import com.rideapp.booking.dto.RideResponse;
import com.rideapp.booking.entity.Ride;
import com.rideapp.booking.entity.RideStatus;
import com.rideapp.common.exception.BusinessRuleException;
import com.rideapp.common.exception.ResourceNotFoundException;
import com.rideapp.driver.entity.Driver;
import com.rideapp.driver.entity.OperationalStatus;
import com.rideapp.driver.repository.DriverRepository;
import com.rideapp.messaging.event.RideCancelledEvent;
import com.rideapp.messaging.event.RideRequestedEvent;
import com.rideapp.messaging.event.TripCompletedEvent;
import com.rideapp.messaging.event.TripStartedEvent;
import com.rideapp.messaging.outbox.OutboxService;
import com.rideapp.pricing.dto.RideQuoteRequest;
import com.rideapp.pricing.dto.RideQuoteResponse;
import com.rideapp.pricing.entity.FareBreakdown;
import com.rideapp.pricing.repository.FareBreakdownRepository;
import com.rideapp.pricing.service.PricingService;
import com.rideapp.user.entity.User;
import com.rideapp.user.repository.UserRepository;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RideBookingService {

    private static final Logger log = LoggerFactory.getLogger(RideBookingService.class);

    private final com.rideapp.booking.repository.RideRepository rideRepository;
    private final FareBreakdownRepository fareBreakdownRepository;
    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final PricingService pricingService;
    private final OutboxService outboxService;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    public RideBookingService(
            com.rideapp.booking.repository.RideRepository rideRepository,
            FareBreakdownRepository fareBreakdownRepository,
            UserRepository userRepository,
            DriverRepository driverRepository,
            PricingService pricingService,
            OutboxService outboxService) {
        this.rideRepository = rideRepository;
        this.fareBreakdownRepository = fareBreakdownRepository;
        this.userRepository = userRepository;
        this.driverRepository = driverRepository;
        this.pricingService = pricingService;
        this.outboxService = outboxService;
    }

    @Transactional
    public RideResponse bookRide(UUID passengerId, CreateRideRequest request) {
        // Idempotency check: Return existing ride if key already exists
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
            Optional<Ride> existingRide = rideRepository.findByIdempotencyKey(request.getIdempotencyKey());
            if (existingRide.isPresent()) {
                log.info("Returning existing ride for idempotency key: {}", request.getIdempotencyKey());
                return RideResponse.fromEntity(existingRide.get());
            }
        }

        User passenger = userRepository.findById(passengerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", passengerId));

        // Generate fare estimate
        RideQuoteRequest quoteReq = new RideQuoteRequest(
                request.getPickupAddress(), request.getPickupLatitude(), request.getPickupLongitude(),
                request.getDropoffAddress(), request.getDropoffLatitude(), request.getDropoffLongitude(),
                request.getRideType()
        );
        RideQuoteResponse quote = pricingService.generateQuote(quoteReq);

        Ride ride = new Ride();
        ride.setPassenger(passenger);
        ride.setRideType(request.getRideType());
        ride.setStatus(RideStatus.REQUESTED);
        ride.setPickupAddress(request.getPickupAddress());
        ride.setPickupGeom(geometryFactory.createPoint(new Coordinate(request.getPickupLongitude(), request.getPickupLatitude())));
        ride.setDropoffAddress(request.getDropoffAddress());
        ride.setDropoffGeom(geometryFactory.createPoint(new Coordinate(request.getDropoffLongitude(), request.getDropoffLatitude())));
        ride.setEstimatedDistanceKm(quote.getEstimatedDistanceKm());
        ride.setEstimatedDurationMin(quote.getEstimatedDurationMin());
        ride.setIdempotencyKey(request.getIdempotencyKey());

        ride = rideRepository.save(ride);

        FareBreakdown fare = new FareBreakdown(
                ride,
                quote.getBaseFare(),
                quote.getDistanceFare(),
                quote.getTimeFare(),
                quote.getSurgeMultiplier(),
                BigDecimal.ZERO,
                quote.getSoloFare(),
                quote.getCurrency()
        );
        fareBreakdownRepository.save(fare);
        ride.setFareBreakdown(fare);

        // Record outbox event for Kafka event streaming (atomically in same PostgreSQL transaction)
        RideRequestedEvent event = RideRequestedEvent.of(
                ride.getId(),
                passenger.getId(),
                ride.getRideType(),
                request.getPickupLongitude(),
                request.getPickupLatitude(),
                request.getDropoffLongitude(),
                request.getDropoffLatitude(),
                quote.getSoloFare()
        );
        outboxService.recordEvent(
                "Ride",
                ride.getId().toString(),
                "RideRequestedEvent",
                "ride.events",
                ride.getId().toString(),
                event
        );

        return RideResponse.fromEntity(ride);
    }

    @Transactional
    public RideResponse cancelRide(UUID rideId, UUID actorId, String reason) {
        Ride ride = findRideById(rideId);

        if (ride.getStatus() == RideStatus.COMPLETED) {
            throw new BusinessRuleException("Completed ride cannot be cancelled");
        }
        if (ride.getStatus() == RideStatus.IN_PROGRESS) {
            throw new BusinessRuleException("Ride is already in progress and cannot be cancelled directly");
        }

        ride.transitionTo(RideStatus.CANCELLED);
        ride.setCancellationReason(reason);
        ride.setCancelledBy(actorId.toString());

        // Free up assigned driver if any
        if (ride.getDriver() != null) {
            Driver driver = ride.getDriver();
            if (driver.getOperationalStatus() == OperationalStatus.ON_TRIP) {
                driver.setOperationalStatus(OperationalStatus.AVAILABLE);
                driverRepository.save(driver);
            }
        }

        ride = rideRepository.save(ride);

        // Publish outbox event
        outboxService.recordEvent(
                "Ride",
                ride.getId().toString(),
                "RideCancelledEvent",
                "ride.events",
                ride.getId().toString(),
                RideCancelledEvent.of(rideId, actorId, reason)
        );

        return RideResponse.fromEntity(ride);
    }

    @Transactional
    public RideResponse startRide(UUID rideId, UUID driverId) {
        Ride ride = findRideById(rideId);

        if (ride.getDriver() == null || !ride.getDriver().getId().equals(driverId)) {
            throw new BusinessRuleException("Only the assigned driver can start the ride");
        }

        ride.transitionTo(RideStatus.IN_PROGRESS);
        Driver driver = ride.getDriver();
        driver.setOperationalStatus(OperationalStatus.ON_TRIP);
        driverRepository.save(driver);

        ride = rideRepository.save(ride);

        outboxService.recordEvent(
                "Ride",
                ride.getId().toString(),
                "TripStartedEvent",
                "ride.events",
                ride.getId().toString(),
                TripStartedEvent.of(rideId, driverId, ride.getStartedAt())
        );

        return RideResponse.fromEntity(ride);
    }

    @Transactional
    public RideResponse completeRide(UUID rideId, UUID driverId, BigDecimal actualDistanceKm, Integer actualDurationMin) {
        Ride ride = findRideById(rideId);

        if (ride.getDriver() == null || !ride.getDriver().getId().equals(driverId)) {
            throw new BusinessRuleException("Only the assigned driver can complete the ride");
        }

        ride.transitionTo(RideStatus.COMPLETED);
        ride.setActualDistanceKm(actualDistanceKm != null ? actualDistanceKm : ride.getEstimatedDistanceKm());
        ride.setActualDurationMin(actualDurationMin != null ? actualDurationMin : ride.getEstimatedDurationMin());

        Driver driver = ride.getDriver();
        driver.setOperationalStatus(OperationalStatus.AVAILABLE);
        driver.setTotalTrips(driver.getTotalTrips() + 1);
        driverRepository.save(driver);

        ride = rideRepository.save(ride);

        BigDecimal finalFare = ride.getFareBreakdown() != null ? ride.getFareBreakdown().getFinalFare() : BigDecimal.ZERO;

        outboxService.recordEvent(
                "Ride",
                ride.getId().toString(),
                "TripCompletedEvent",
                "ride.events",
                ride.getId().toString(),
                TripCompletedEvent.of(
                        rideId,
                        ride.getPassenger().getId(),
                        driverId,
                        finalFare,
                        ride.getActualDistanceKm(),
                        ride.getActualDurationMin(),
                        ride.getCompletedAt()
                )
        );

        return RideResponse.fromEntity(ride);
    }

    @Transactional(readOnly = true)
    public RideResponse getRideDetails(UUID rideId) {
        return RideResponse.fromEntity(findRideById(rideId));
    }

    @Transactional(readOnly = true)
    public List<RideResponse> getPassengerRides(UUID passengerId) {
        return rideRepository.findByPassengerIdOrderByCreatedAtDesc(passengerId).stream()
                .map(RideResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public Ride findRideById(UUID rideId) {
        return rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride", rideId));
    }
}
