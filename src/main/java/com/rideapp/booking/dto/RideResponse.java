package com.rideapp.booking.dto;

import com.rideapp.booking.entity.Ride;
import com.rideapp.booking.entity.RideStatus;
import com.rideapp.booking.entity.RideType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class RideResponse {

    private UUID id;
    private UUID passengerId;
    private String passengerName;
    private UUID driverId;
    private String driverName;
    private RideType rideType;
    private RideStatus status;
    private String pickupAddress;
    private String dropoffAddress;
    private BigDecimal estimatedDistanceKm;
    private int estimatedDurationMin;
    private BigDecimal actualDistanceKm;
    private Integer actualDurationMin;
    private BigDecimal fare;
    private String currency;
    private String cancellationReason;
    private String idempotencyKey;
    private Instant createdAt;
    private Instant startedAt;
    private Instant completedAt;

    public RideResponse() {
    }

    public static RideResponse fromEntity(Ride ride) {
        RideResponse res = new RideResponse();
        res.setId(ride.getId());
        res.setPassengerId(ride.getPassenger().getId());
        res.setPassengerName(ride.getPassenger().getFullName());
        if (ride.getDriver() != null) {
            res.setDriverId(ride.getDriver().getId());
            res.setDriverName(ride.getDriver().getUser().getFullName());
        }
        res.setRideType(ride.getRideType());
        res.setStatus(ride.getStatus());
        res.setPickupAddress(ride.getPickupAddress());
        res.setDropoffAddress(ride.getDropoffAddress());
        res.setEstimatedDistanceKm(ride.getEstimatedDistanceKm());
        res.setEstimatedDurationMin(ride.getEstimatedDurationMin());
        res.setActualDistanceKm(ride.getActualDistanceKm());
        res.setActualDurationMin(ride.getActualDurationMin());
        if (ride.getFareBreakdown() != null) {
            res.setFare(ride.getFareBreakdown().getFinalFare());
            res.setCurrency(ride.getFareBreakdown().getCurrency());
        }
        res.setCancellationReason(ride.getCancellationReason());
        res.setIdempotencyKey(ride.getIdempotencyKey());
        res.setCreatedAt(ride.getCreatedAt());
        res.setStartedAt(ride.getStartedAt());
        res.setCompletedAt(ride.getCompletedAt());
        return res;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getPassengerId() {
        return passengerId;
    }

    public void setPassengerId(UUID passengerId) {
        this.passengerId = passengerId;
    }

    public String getPassengerName() {
        return passengerName;
    }

    public void setPassengerName(String passengerName) {
        this.passengerName = passengerName;
    }

    public UUID getDriverId() {
        return driverId;
    }

    public void setDriverId(UUID driverId) {
        this.driverId = driverId;
    }

    public String getDriverName() {
        return driverName;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }

    public RideType getRideType() {
        return rideType;
    }

    public void setRideType(RideType rideType) {
        this.rideType = rideType;
    }

    public RideStatus getStatus() {
        return status;
    }

    public void setStatus(RideStatus status) {
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

    public int getEstimatedDurationMin() {
        return estimatedDurationMin;
    }

    public void setEstimatedDurationMin(int estimatedDurationMin) {
        this.estimatedDurationMin = estimatedDurationMin;
    }

    public BigDecimal getActualDistanceKm() {
        return actualDistanceKm;
    }

    public void setActualDistanceKm(BigDecimal actualDistanceKm) {
        this.actualDistanceKm = actualDistanceKm;
    }

    public Integer getActualDurationMin() {
        return actualDurationMin;
    }

    public void setActualDurationMin(Integer actualDurationMin) {
        this.actualDurationMin = actualDurationMin;
    }

    public BigDecimal getFare() {
        return fare;
    }

    public void setFare(BigDecimal fare) {
        this.fare = fare;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
