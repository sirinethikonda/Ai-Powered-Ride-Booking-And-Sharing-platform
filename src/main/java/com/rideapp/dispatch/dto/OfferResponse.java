package com.rideapp.dispatch.dto;

import com.rideapp.dispatch.entity.DriverOffer;
import com.rideapp.dispatch.entity.OfferStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class OfferResponse {

    private UUID offerId;
    private UUID rideId;
    private OfferStatus status;
    private String pickupAddress;
    private String dropoffAddress;
    private BigDecimal estimatedDistanceKm;
    private BigDecimal estimatedEarnings;
    private int attemptNumber;
    private Instant expiresAt;

    public OfferResponse() {
    }

    public static OfferResponse fromEntity(DriverOffer offer) {
        OfferResponse res = new OfferResponse();
        res.setOfferId(offer.getId());
        res.setRideId(offer.getRide().getId());
        res.setStatus(offer.getStatus());
        res.setPickupAddress(offer.getRide().getPickupAddress());
        res.setDropoffAddress(offer.getRide().getDropoffAddress());
        res.setEstimatedDistanceKm(offer.getRide().getEstimatedDistanceKm());
        res.setEstimatedEarnings(offer.getEstimatedEarnings());
        res.setAttemptNumber(offer.getAttemptNumber());
        res.setExpiresAt(offer.getExpiresAt());
        return res;
    }

    public UUID getOfferId() {
        return offerId;
    }

    public void setOfferId(UUID offerId) {
        this.offerId = offerId;
    }

    public UUID getRideId() {
        return rideId;
    }

    public void setRideId(UUID rideId) {
        this.rideId = rideId;
    }

    public OfferStatus getStatus() {
        return status;
    }

    public void setStatus(OfferStatus status) {
        this.status = status;
    }

    public String getPickupAddress() {
        return pickupAddress;
    }

    public void setPickupAddress(String pickupAddress) {
        this.pickupAddress = pickupAddress;
    }

    public String getDropoffAddress() {
        return dropoffAddress;
    }

    public void setDropoffAddress(String dropoffAddress) {
        this.dropoffAddress = dropoffAddress;
    }

    public BigDecimal getEstimatedDistanceKm() {
        return estimatedDistanceKm;
    }

    public void setEstimatedDistanceKm(BigDecimal estimatedDistanceKm) {
        this.estimatedDistanceKm = estimatedDistanceKm;
    }

    public BigDecimal getEstimatedEarnings() {
        return estimatedEarnings;
    }

    public void setEstimatedEarnings(BigDecimal estimatedEarnings) {
        this.estimatedEarnings = estimatedEarnings;
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public void setAttemptNumber(int attemptNumber) {
        this.attemptNumber = attemptNumber;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
