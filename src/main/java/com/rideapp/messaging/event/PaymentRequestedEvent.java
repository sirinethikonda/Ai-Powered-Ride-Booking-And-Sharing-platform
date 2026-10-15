package com.rideapp.messaging.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentRequestedEvent(
        UUID eventId,
        UUID paymentId,
        UUID rideId,
        UUID passengerId,
        BigDecimal amount,
        String currency,
        String idempotencyKey,
        Instant timestamp
) {
    public static PaymentRequestedEvent of(UUID paymentId, UUID rideId, UUID passengerId, BigDecimal amount, String currency, String idempotencyKey) {
        return new PaymentRequestedEvent(UUID.randomUUID(), paymentId, rideId, passengerId, amount, currency, idempotencyKey, Instant.now());
    }
}
