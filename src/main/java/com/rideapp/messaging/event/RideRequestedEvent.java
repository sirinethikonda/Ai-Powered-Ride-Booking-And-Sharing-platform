package com.rideapp.messaging.event;

import com.rideapp.booking.entity.RideType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RideRequestedEvent(
        UUID eventId,
        UUID rideId,
        UUID passengerId,
        RideType rideType,
        double pickupLongitude,
        double pickupLatitude,
        double dropoffLongitude,
        double dropoffLatitude,
        BigDecimal estimatedFare,
        Instant timestamp
) {
    public static RideRequestedEvent of(UUID rideId, UUID passengerId, RideType rideType,
                                        double pickupLon, double pickupLat, double dropoffLon, double dropoffLat,
                                        BigDecimal estimatedFare) {
        return new RideRequestedEvent(UUID.randomUUID(), rideId, passengerId, rideType,
                pickupLon, pickupLat, dropoffLon, dropoffLat, estimatedFare, Instant.now());
    }
}
