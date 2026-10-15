package com.rideapp.booking;

import com.rideapp.booking.entity.Ride;
import com.rideapp.booking.entity.RideStatus;
import com.rideapp.common.exception.InvalidStateTransitionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RideStateTransitionTest {

    @Test
    @DisplayName("Valid forward lifecycle transitions should succeed")
    void testValidLifecycleTransitions() {
        Ride ride = new Ride();
        assertEquals(RideStatus.REQUESTED, ride.getStatus());

        ride.transitionTo(RideStatus.SEARCHING_FOR_DRIVER);
        assertEquals(RideStatus.SEARCHING_FOR_DRIVER, ride.getStatus());

        ride.transitionTo(RideStatus.DRIVER_ASSIGNED);
        assertEquals(RideStatus.DRIVER_ASSIGNED, ride.getStatus());

        ride.transitionTo(RideStatus.DRIVER_ARRIVING);
        assertEquals(RideStatus.DRIVER_ARRIVING, ride.getStatus());

        ride.transitionTo(RideStatus.DRIVER_ARRIVED);
        assertEquals(RideStatus.DRIVER_ARRIVED, ride.getStatus());

        ride.transitionTo(RideStatus.IN_PROGRESS);
        assertEquals(RideStatus.IN_PROGRESS, ride.getStatus());
        assertNotNull(ride.getStartedAt());

        ride.transitionTo(RideStatus.COMPLETED);
        assertEquals(RideStatus.COMPLETED, ride.getStatus());
        assertNotNull(ride.getCompletedAt());
    }

    @Test
    @DisplayName("Invalid transition from COMPLETED to active state must be rejected")
    void testInvalidTransitionFromCompleted() {
        Ride ride = new Ride();
        ride.setStatus(RideStatus.COMPLETED);

        assertThrows(InvalidStateTransitionException.class, () ->
                ride.transitionTo(RideStatus.SEARCHING_FOR_DRIVER)
        );
        assertThrows(InvalidStateTransitionException.class, () ->
                ride.transitionTo(RideStatus.CANCELLED)
        );
    }

    @Test
    @DisplayName("Direct transition from REQUESTED to IN_PROGRESS must be rejected")
    void testDirectJumpInvalidTransition() {
        Ride ride = new Ride();
        assertEquals(RideStatus.REQUESTED, ride.getStatus());

        assertThrows(InvalidStateTransitionException.class, () ->
                ride.transitionTo(RideStatus.IN_PROGRESS)
        );
    }

    @Test
    @DisplayName("Cancellation is permitted from SEARCHING_FOR_DRIVER")
    void testCancellationPermitted() {
        Ride ride = new Ride();
        ride.setStatus(RideStatus.SEARCHING_FOR_DRIVER);

        ride.transitionTo(RideStatus.CANCELLED);
        assertEquals(RideStatus.CANCELLED, ride.getStatus());
        assertTrue(ride.getStatus().isTerminal());
    }
}
