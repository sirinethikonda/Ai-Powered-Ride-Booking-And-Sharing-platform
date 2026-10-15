package com.rideapp.messaging.event;

import java.time.Instant;
import java.util.UUID;

public record DriverAssignedEvent(
        UUID eventId,
        UUID rideId,
        UUID driverId,
        UUID vehicleId,
        Instant timestamp
) {
    public static DriverAssignedEvent of(UUID rideId, UUID driverId, UUID vehicleId) {
        return new DriverAssignedEvent(UUID.randomUUID(), rideId, driverId, vehicleId, Instant.now());
    }
}
