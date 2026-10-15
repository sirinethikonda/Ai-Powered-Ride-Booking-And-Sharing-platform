package com.rideapp.messaging.event;

import java.time.Instant;
import java.util.UUID;

public record DriverReplacementRequestedEvent(
        UUID eventId,
        UUID rideId,
        UUID previousDriverId,
        String reason,
        int attemptNumber,
        Instant timestamp
) {
    public static DriverReplacementRequestedEvent of(UUID rideId, UUID previousDriverId, String reason, int attemptNumber) {
        return new DriverReplacementRequestedEvent(UUID.randomUUID(), rideId, previousDriverId, reason, attemptNumber, Instant.now());
    }
}
