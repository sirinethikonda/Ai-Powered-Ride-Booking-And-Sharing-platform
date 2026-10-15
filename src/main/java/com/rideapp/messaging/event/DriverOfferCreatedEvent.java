package com.rideapp.messaging.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DriverOfferCreatedEvent(
        UUID eventId,
        UUID offerId,
        UUID rideId,
        UUID driverId,
        BigDecimal estimatedEarnings,
        Instant expiresAt,
        Instant timestamp
) {
    public static DriverOfferCreatedEvent of(UUID offerId, UUID rideId, UUID driverId, BigDecimal earnings, Instant expiresAt) {
        return new DriverOfferCreatedEvent(UUID.randomUUID(), offerId, rideId, driverId, earnings, expiresAt, Instant.now());
    }
}
