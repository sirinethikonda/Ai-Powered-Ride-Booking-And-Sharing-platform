package com.rideapp.messaging.event;

import java.time.Instant;
import java.util.UUID;

public record DriverOfferRejectedEvent(
        UUID eventId,
        UUID offerId,
        UUID rideId,
        UUID driverId,
        String reason,
        Instant timestamp
) {
    public static DriverOfferRejectedEvent of(UUID offerId, UUID rideId, UUID driverId, String reason) {
        return new DriverOfferRejectedEvent(UUID.randomUUID(), offerId, rideId, driverId, reason, Instant.now());
    }
}
