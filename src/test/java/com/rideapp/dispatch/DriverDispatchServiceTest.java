package com.rideapp.dispatch;

import com.rideapp.booking.entity.Ride;
import com.rideapp.booking.entity.RideStatus;
import com.rideapp.booking.repository.RideRepository;
import com.rideapp.common.exception.InvalidStateTransitionException;
import com.rideapp.dispatch.dto.OfferResponse;
import com.rideapp.dispatch.entity.AssignmentStatus;
import com.rideapp.dispatch.entity.DriverOffer;
import com.rideapp.dispatch.entity.OfferStatus;
import com.rideapp.dispatch.entity.RideAssignment;
import com.rideapp.dispatch.repository.DriverOfferRepository;
import com.rideapp.dispatch.repository.RideAssignmentRepository;
import com.rideapp.dispatch.service.DriverDispatchService;
import com.rideapp.driver.entity.Driver;
import com.rideapp.driver.repository.DriverRepository;
import com.rideapp.driver.repository.VehicleRepository;
import com.rideapp.location.service.DriverLocationService;
import com.rideapp.messaging.outbox.OutboxService;
import com.rideapp.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverDispatchServiceTest {

    @Mock
    private RideRepository rideRepository;
    @Mock
    private DriverOfferRepository driverOfferRepository;
    @Mock
    private RideAssignmentRepository rideAssignmentRepository;
    @Mock
    private DriverRepository driverRepository;
    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private DriverLocationService driverLocationService;
    @Mock
    private OutboxService outboxService;

    private DriverDispatchService dispatchService;

    private UUID driverId;
    private UUID rideId;
    private UUID offerId;
    private Driver driver;
    private Ride ride;
    private DriverOffer offer;

    @BeforeEach
    void setUp() {
        dispatchService = new DriverDispatchService(
                rideRepository,
                driverOfferRepository,
                rideAssignmentRepository,
                driverRepository,
                vehicleRepository,
                driverLocationService,
                outboxService,
                30,
                3,
                5.0
        );

        driverId = UUID.randomUUID();
        rideId = UUID.randomUUID();
        offerId = UUID.randomUUID();

        User user = new User();
        user.setId(driverId);
        user.setFullName("Ramesh Driver");

        driver = new Driver();
        driver.setId(driverId);
        driver.setUser(user);

        ride = new Ride();
        ride.setId(rideId);
        ride.setStatus(RideStatus.SEARCHING_FOR_DRIVER);
        org.locationtech.jts.geom.GeometryFactory gf = new org.locationtech.jts.geom.GeometryFactory();
        ride.setPickupGeom(gf.createPoint(new org.locationtech.jts.geom.Coordinate(77.5946, 12.9716)));

        offer = new DriverOffer(ride, driver, 1, new BigDecimal("120.00"), Instant.now().plusSeconds(30));
        offer.setId(offerId);
    }

    @Test
    @DisplayName("Driver accepting valid offer succeeds and transitions ride to DRIVER_ASSIGNED")
    void testAcceptValidOffer() {
        when(driverOfferRepository.findById(offerId)).thenReturn(Optional.of(offer));
        when(rideAssignmentRepository.findByRideIdAndStatus(rideId, AssignmentStatus.ACTIVE)).thenReturn(Optional.empty());
        when(vehicleRepository.findByDriverIdAndIsActiveTrue(driverId)).thenReturn(Collections.emptyList());
        when(driverOfferRepository.findByRideId(rideId)).thenReturn(Collections.singletonList(offer));

        OfferResponse response = dispatchService.acceptOffer(offerId, driverId);

        assertNotNull(response);
        assertEquals(OfferStatus.ACCEPTED, offer.getStatus());
        assertEquals(RideStatus.DRIVER_ASSIGNED, ride.getStatus());
        assertEquals(driver, ride.getDriver());

        verify(rideAssignmentRepository, times(1)).save(any(RideAssignment.class));
        verify(driverLocationService, times(1)).removeDriverFromGeo(driverId);
        verify(outboxService, times(1)).recordEvent(eq("Ride"), any(), eq("DriverAssignedEvent"), any(), any(), any());
    }

    @Test
    @DisplayName("Accepting expired offer triggers replacement and throws InvalidStateTransitionException")
    void testAcceptExpiredOfferFailsAndReplaces() {
        offer.setExpiresAt(Instant.now().minusSeconds(10)); // Expired
        when(driverOfferRepository.findById(offerId)).thenReturn(Optional.of(offer));
        when(rideRepository.findById(rideId)).thenReturn(Optional.of(ride));

        assertThrows(InvalidStateTransitionException.class, () ->
                dispatchService.acceptOffer(offerId, driverId)
        );

        assertEquals(OfferStatus.EXPIRED, offer.getStatus());
    }

    @Test
    @DisplayName("Driver rejection triggers automatic replacement workflow")
    void testRejectOfferTriggersReplacement() {
        when(driverOfferRepository.findById(offerId)).thenReturn(Optional.of(offer));
        when(rideRepository.findById(rideId)).thenReturn(Optional.of(ride));

        OfferResponse response = dispatchService.rejectOffer(offerId, driverId, "Too far from pickup");

        assertEquals(OfferStatus.REJECTED, offer.getStatus());
        verify(outboxService, times(1)).recordEvent(eq("DriverOffer"), any(), eq("DriverOfferRejectedEvent"), any(), any(), any());
    }

    @Test
    @DisplayName("Reaching maximum replacement attempts (3) terminates ride in FAILED status")
    void testExhaustedReplacementAttemptsFailsRide() {
        when(rideRepository.findById(rideId)).thenReturn(Optional.of(ride));

        dispatchService.triggerDriverReplacement(rideId, driverId, "Driver rejected", 3);

        assertEquals(RideStatus.FAILED, ride.getStatus());
        verify(rideRepository, times(1)).save(ride);
    }
}
