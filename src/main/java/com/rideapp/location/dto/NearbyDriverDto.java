package com.rideapp.location.dto;

import com.rideapp.driver.entity.VehicleCategory;

import java.math.BigDecimal;
import java.util.UUID;

public class NearbyDriverDto {

    private UUID driverId;
    private double latitude;
    private double longitude;
    private double distanceKm;
    private BigDecimal rating;
    private VehicleCategory vehicleCategory;

    public NearbyDriverDto() {
    }

    public NearbyDriverDto(UUID driverId, double latitude, double longitude, double distanceKm, BigDecimal rating, VehicleCategory vehicleCategory) {
        this.driverId = driverId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.distanceKm = distanceKm;
        this.rating = rating;
        this.vehicleCategory = vehicleCategory;
    }

    public UUID getDriverId() {
        return driverId;
    }

    public void setDriverId(UUID driverId) {
        this.driverId = driverId;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public BigDecimal getRating() {
        return rating;
    }

    public void setRating(BigDecimal rating) {
        this.rating = rating;
    }

    public VehicleCategory getVehicleCategory() {
        return vehicleCategory;
    }

    public void setVehicleCategory(VehicleCategory vehicleCategory) {
        this.vehicleCategory = vehicleCategory;
    }
}
