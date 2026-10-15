package com.rideapp.messaging.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SharedRideMatchConfirmedEvent(
        UUID eventId,
        UUID sharedGroupId,
        UUID primaryRideId,
        UUID secondaryRideId,
        BigDecimal primaryPassengerSharedFare,
        BigDecimal secondaryPassengerSharedFare,
        Instant timestamp
) {
    public static SharedRideMatchConfirmedEvent of(UUID sharedGroupId, UUID primaryRideId, UUID secondaryRideId,
                                                   BigDecimal primaryFare, BigDecimal secondaryFare) {
        return new SharedRideMatchConfirmedEvent(UUID.randomUUID(), sharedGroupId, primaryRideId, secondaryRideId,
                primaryFare, secondaryFare, Instant.now());
    }
}
