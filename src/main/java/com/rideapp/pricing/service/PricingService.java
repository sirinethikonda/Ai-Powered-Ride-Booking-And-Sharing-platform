package com.rideapp.pricing.service;

import com.rideapp.pricing.dto.RideQuoteRequest;
import com.rideapp.pricing.dto.RideQuoteResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class PricingService {

    private final BigDecimal baseFare = new BigDecimal("50.00");
    private final BigDecimal perKmRate = new BigDecimal("12.00");
    private final BigDecimal perMinuteRate = new BigDecimal("2.00");
    private final BigDecimal sharedDiscountFactor;

    public PricingService(@Value("${app.shared-ride.shared-discount-factor:0.70}") double discountFactor) {
        this.sharedDiscountFactor = BigDecimal.valueOf(discountFactor);
    }

    public RideQuoteResponse generateQuote(RideQuoteRequest request) {
        double distanceKm = calculateHaversineDistance(
                request.getPickupLatitude(), request.getPickupLongitude(),
                request.getDropoffLatitude(), request.getDropoffLongitude()
        );

        // Ensure minimum 1.0 km distance
        distanceKm = Math.max(distanceKm, 1.0);
        BigDecimal distance = BigDecimal.valueOf(distanceKm).setScale(2, RoundingMode.HALF_UP);

        // Estimate duration: ~2.5 min/km + 3 min buffer
        int durationMin = (int) Math.ceil(distanceKm * 2.5 + 3);

        BigDecimal distanceFare = distance.multiply(perKmRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal timeFare = BigDecimal.valueOf(durationMin).multiply(perMinuteRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal surgeMultiplier = BigDecimal.valueOf(1.00).setScale(2, RoundingMode.HALF_UP);

        BigDecimal subtotal = baseFare.add(distanceFare).add(timeFare);
        BigDecimal soloFare = subtotal.multiply(surgeMultiplier).setScale(2, RoundingMode.HALF_UP);

        BigDecimal conditionalSharedFare = soloFare.multiply(sharedDiscountFactor).setScale(2, RoundingMode.HALF_UP);

        RideQuoteResponse response = new RideQuoteResponse();
        response.setQuoteId(UUID.randomUUID());
        response.setEstimatedDistanceKm(distance);
        response.setEstimatedDurationMin(durationMin);
        response.setBaseFare(baseFare);
        response.setDistanceFare(distanceFare);
        response.setTimeFare(timeFare);
        response.setSurgeMultiplier(surgeMultiplier);
        response.setSoloFare(soloFare);
        response.setConditionalSharedFare(conditionalSharedFare);
        response.setConditionalPricingNotice(
                String.format("The shared fare discount applies conditionally upon a compatible gender & route match before trip start. If no compatible passenger joins, the disclosed solo fare of ₹%s applies.", soloFare)
        );
        response.setCurrency("INR");
        response.setExpiresAt(Instant.now().plus(15, ChronoUnit.MINUTES));

        return response;
    }

    public double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth radius in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
