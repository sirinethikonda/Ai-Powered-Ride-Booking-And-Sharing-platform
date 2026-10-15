package com.rideapp.messaging.event;

import java.time.Instant;
import java.util.UUID;

public record TripStartedEvent(
        UUID eventId,
        UUID rideId,
        UUID driverId,
        Instant startedAt,
        Instant timestamp
) {
    public static TripStartedEvent of(UUID rideId, UUID driverId, Instant startedAt) {
        return new TripStartedEvent(UUID.randomUUID(), rideId, driverId, startedAt, Instant.now());
    }
}
