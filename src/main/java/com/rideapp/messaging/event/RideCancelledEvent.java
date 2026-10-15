package com.rideapp.messaging.event;

import java.time.Instant;
import java.util.UUID;

public record RideCancelledEvent(
        UUID eventId,
        UUID rideId,
        UUID actorId,
        String reason,
        Instant timestamp
) {
    public static RideCancelledEvent of(UUID rideId, UUID actorId, String reason) {
        return new RideCancelledEvent(UUID.randomUUID(), rideId, actorId, reason, Instant.now());
    }
}
