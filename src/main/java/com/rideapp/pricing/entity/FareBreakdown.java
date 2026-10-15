package com.rideapp.pricing.entity;

import com.rideapp.booking.entity.Ride;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fare_breakdowns")
public class FareBreakdown {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ride_id", nullable = false, unique = true)
    private Ride ride;

    @Column(name = "base_fare", nullable = false, precision = 10, scale = 2)
    private BigDecimal baseFare;

    @Column(name = "distance_fare", nullable = false, precision = 10, scale = 2)
    private BigDecimal distanceFare;

    @Column(name = "time_fare", nullable = false, precision = 10, scale = 2)
    private BigDecimal timeFare;

    @Column(name = "surge_multiplier", nullable = false, precision = 4, scale = 2)
    private BigDecimal surgeMultiplier = BigDecimal.ONE;

    @Column(name = "shared_discount_applied", nullable = false, precision = 10, scale = 2)
    private BigDecimal sharedDiscountApplied = BigDecimal.ZERO;

    @Column(name = "final_fare", nullable = false, precision = 10, scale = 2)
    private BigDecimal finalFare;

    @Column(nullable = false, length = 10)
    private String currency = "INR";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public FareBreakdown() {
    }

    public FareBreakdown(Ride ride, BigDecimal baseFare, BigDecimal distanceFare, BigDecimal timeFare,
                         BigDecimal surgeMultiplier, BigDecimal sharedDiscountApplied, BigDecimal finalFare, String currency) {
        this.ride = ride;
        this.baseFare = baseFare;
        this.distanceFare = distanceFare;
        this.timeFare = timeFare;
        this.surgeMultiplier = surgeMultiplier;
        this.sharedDiscountApplied = sharedDiscountApplied;
        this.finalFare = finalFare;
        this.currency = currency;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Ride getRide() {
        return ride;
    }

    public void setRide(Ride ride) {
        this.ride = ride;
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

    public BigDecimal getSharedDiscountApplied() {
        return sharedDiscountApplied;
    }

    public void setSharedDiscountApplied(BigDecimal sharedDiscountApplied) {
        this.sharedDiscountApplied = sharedDiscountApplied;
    }

    public BigDecimal getFinalFare() {
        return finalFare;
    }

    public void setFinalFare(BigDecimal finalFare) {
        this.finalFare = finalFare;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
