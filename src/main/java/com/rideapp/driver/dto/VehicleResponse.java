package com.rideapp.driver.dto;

import com.rideapp.driver.entity.Vehicle;
import com.rideapp.driver.entity.VehicleCategory;

import java.util.UUID;

public class VehicleResponse {

    private UUID id;
    private String make;
    private String model;
    private String licensePlate;
    private String color;
    private VehicleCategory category;
    private int seatCapacity;
    private boolean active;

    public VehicleResponse() {
    }

    public static VehicleResponse fromEntity(Vehicle vehicle) {
        VehicleResponse response = new VehicleResponse();
        response.setId(vehicle.getId());
        response.setMake(vehicle.getMake());
        response.setModel(vehicle.getModel());
        response.setLicensePlate(vehicle.getLicensePlate());
        response.setColor(vehicle.getColor());
        response.setCategory(vehicle.getCategory());
        response.setSeatCapacity(vehicle.getSeatCapacity());
        response.setActive(vehicle.isActive());
        return response;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getMake() {
        return make;
    }

    public void setMake(String make) {
        this.make = make;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public VehicleCategory getCategory() {
        return category;
    }

    public void setCategory(VehicleCategory category) {
        this.category = category;
    }

    public int getSeatCapacity() {
        return seatCapacity;
    }

    public void setSeatCapacity(int seatCapacity) {
        this.seatCapacity = seatCapacity;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
