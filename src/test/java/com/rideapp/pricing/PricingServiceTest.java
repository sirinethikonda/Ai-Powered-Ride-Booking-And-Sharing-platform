package com.rideapp.pricing;

import com.rideapp.booking.entity.RideType;
import com.rideapp.pricing.dto.RideQuoteRequest;
import com.rideapp.pricing.dto.RideQuoteResponse;
import com.rideapp.pricing.service.PricingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PricingServiceTest {

    private PricingService pricingService;

    @BeforeEach
    void setUp() {
        pricingService = new PricingService(0.70);
    }

    @Test
    @DisplayName("Haversine distance calculation is accurate between coordinates")
    void testHaversineDistance() {
        // Distance between Bangalore Majestic (12.9767, 77.5713) and Indiranagar (12.9784, 77.6408) is ~7.5 km
        double distance = pricingService.calculateHaversineDistance(12.9767, 77.5713, 12.9784, 77.6408);
        assertTrue(distance > 7.0 && distance < 8.0, "Distance should be ~7.5km, got: " + distance);
    }

    @Test
    @DisplayName("Quote produces positive solo fare and 30% conditional shared discount")
    void testGenerateQuote() {
        RideQuoteRequest req = new RideQuoteRequest(
                "Majestic", 12.9767, 77.5713,
                "Indiranagar", 12.9784, 77.6408,
                RideType.SOLO
        );

        RideQuoteResponse quote = pricingService.generateQuote(req);

        assertNotNull(quote.getQuoteId());
        assertTrue(quote.getEstimatedDistanceKm().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(quote.getEstimatedDurationMin() > 0);
        assertTrue(quote.getSoloFare().compareTo(BigDecimal.ZERO) > 0);

        // Shared fare should equal 70% of solo fare
        BigDecimal expectedSharedFare = quote.getSoloFare().multiply(new BigDecimal("0.70")).setScale(2, java.math.RoundingMode.HALF_UP);
        assertEquals(expectedSharedFare, quote.getConditionalSharedFare());

        // Disclosure notice must be present
        assertNotNull(quote.getConditionalPricingNotice());
        assertTrue(quote.getConditionalPricingNotice().contains("disclosed solo fare"));
    }
}
