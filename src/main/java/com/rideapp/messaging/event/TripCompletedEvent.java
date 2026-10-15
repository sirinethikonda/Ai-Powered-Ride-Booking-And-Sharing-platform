package com.rideapp.messaging.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TripCompletedEvent(
        UUID eventId,
        UUID rideId,
        UUID passengerId,
        UUID driverId,
        BigDecimal finalFare,
        BigDecimal actualDistanceKm,
        int actualDurationMin,
        Instant completedAt,
        Instant timestamp
) {
    public static TripCompletedEvent of(UUID rideId, UUID passengerId, UUID driverId,
                                        BigDecimal finalFare, BigDecimal actualDistanceKm, int actualDurationMin,
                                        Instant completedAt) {
        return new TripCompletedEvent(UUID.randomUUID(), rideId, passengerId, driverId,
                finalFare, actualDistanceKm, actualDurationMin, completedAt, Instant.now());
    }
}
