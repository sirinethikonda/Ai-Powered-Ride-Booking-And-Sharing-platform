package com.rideapp.pricing.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class RideQuoteResponse {

    private UUID quoteId;
    private BigDecimal estimatedDistanceKm;
    private int estimatedDurationMin;
    private BigDecimal baseFare;
    private BigDecimal distanceFare;
    private BigDecimal timeFare;
    private BigDecimal surgeMultiplier;
    private BigDecimal soloFare;
    private BigDecimal conditionalSharedFare;
    private String conditionalPricingNotice;
    private String currency;
    private Instant expiresAt;

    public RideQuoteResponse() {
    }

    public UUID getQuoteId() {
        return quoteId;
    }

    public void setQuoteId(UUID quoteId) {
        this.quoteId = quoteId;
    }

    public BigDecimal getEstimatedDistanceKm() {
        return estimatedDistanceKm;
    }

    public void setEstimatedDistanceKm(BigDecimal estimatedDistanceKm) {
        this.estimatedDistanceKm = estimatedDistanceKm;
    }

    public int getEstimatedDurationMin() {
        return estimatedDurationMin;
    }

    public void setEstimatedDurationMin(int estimatedDurationMin) {
        this.estimatedDurationMin = estimatedDurationMin;
    }

    public BigDecimal getBaseFare() {
        return baseFare;
    }

    public void setBaseFare(BigDecimal baseFare) {
        this.baseFare = baseFare;
    }

    public BigDecimal getDistanceFare() {
        return distanceFare;
    }

    public void setDistanceFare(BigDecimal distanceFare) {
        this.distanceFare = distanceFare;
    }

    public BigDecimal getTimeFare() {
        return timeFare;
    }

    public void setTimeFare(BigDecimal timeFare) {
        this.timeFare = timeFare;
    }

    public BigDecimal getSurgeMultiplier() {
        return surgeMultiplier;
    }

    public void setSurgeMultiplier(BigDecimal surgeMultiplier) {
        this.surgeMultiplier = surgeMultiplier;
    }

    public BigDecimal getSoloFare() {
        return soloFare;
    }

    public void setSoloFare(BigDecimal soloFare) {
        this.soloFare = soloFare;
    }

    public BigDecimal getConditionalSharedFare() {
        return conditionalSharedFare;
    }

    public void setConditionalSharedFare(BigDecimal conditionalSharedFare) {
        this.conditionalSharedFare = conditionalSharedFare;
    }

    public String getConditionalPricingNotice() {
        return conditionalPricingNotice;
    }

    public void setConditionalPricingNotice(String conditionalPricingNotice) {
        this.conditionalPricingNotice = conditionalPricingNotice;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
