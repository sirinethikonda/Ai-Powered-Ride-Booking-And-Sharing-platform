package com.rideapp.driver.dto;

import com.rideapp.driver.entity.Driver;
import com.rideapp.driver.entity.OperationalStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class DriverProfileResponse {

    private UUID id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String licenseNumber;
    private OperationalStatus operationalStatus;
    private BigDecimal rating;
    private int totalTrips;
    private List<VehicleResponse> vehicles;

    public DriverProfileResponse() {
    }

    public static DriverProfileResponse fromEntity(Driver driver) {
        DriverProfileResponse response = new DriverProfileResponse();
        response.setId(driver.getId());
        response.setFullName(driver.getUser().getFullName());
        response.setEmail(driver.getUser().getEmail());
        response.setPhoneNumber(driver.getUser().getPhoneNumber());
        response.setLicenseNumber(driver.getLicenseNumber());
        response.setOperationalStatus(driver.getOperationalStatus());
        response.setRating(driver.getRating());
        response.setTotalTrips(driver.getTotalTrips());
        if (driver.getVehicles() != null) {
            response.setVehicles(driver.getVehicles().stream()
                    .map(VehicleResponse::fromEntity)
                    .collect(Collectors.toList()));
        }
        return response;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public OperationalStatus getOperationalStatus() {
        return operationalStatus;
    }

    public void setOperationalStatus(OperationalStatus operationalStatus) {
        this.operationalStatus = operationalStatus;
    }

    public BigDecimal getRating() {
        return rating;
    }

    public void setRating(BigDecimal rating) {
        this.rating = rating;
    }

    public int getTotalTrips() {
        return totalTrips;
    }

    public void setTotalTrips(int totalTrips) {
        this.totalTrips = totalTrips;
    }

    public List<VehicleResponse> getVehicles() {
        return vehicles;
    }

    public void setVehicles(List<VehicleResponse> vehicles) {
        this.vehicles = vehicles;
    }
}
