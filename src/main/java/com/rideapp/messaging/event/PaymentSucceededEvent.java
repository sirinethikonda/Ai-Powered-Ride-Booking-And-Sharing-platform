package com.rideapp.messaging.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentSucceededEvent(
        UUID eventId,
        UUID paymentId,
        UUID rideId,
        BigDecimal amount,
        String gatewayReference,
        Instant timestamp
) {
    public static PaymentSucceededEvent of(UUID paymentId, UUID rideId, BigDecimal amount, String gatewayRef) {
        return new PaymentSucceededEvent(UUID.randomUUID(), paymentId, rideId, amount, gatewayRef, Instant.now());
    }
}
