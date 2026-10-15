package com.rideapp.messaging.event;

import java.time.Instant;
import java.util.UUID;

public record PaymentFailedEvent(
        UUID eventId,
        UUID paymentId,
        UUID rideId,
        String errorCode,
        String errorMessage,
        Instant timestamp
) {
    public static PaymentFailedEvent of(UUID paymentId, UUID rideId, String errorCode, String errorMessage) {
        return new PaymentFailedEvent(UUID.randomUUID(), paymentId, rideId, errorCode, errorMessage, Instant.now());
    }
}
