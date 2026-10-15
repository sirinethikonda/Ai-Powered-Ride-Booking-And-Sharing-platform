package com.rideapp.driver.dto;

import com.rideapp.driver.entity.VehicleCategory;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class RegisterVehicleRequest {

    @NotBlank(message = "Vehicle make is required")
    private String make;

    @NotBlank(message = "Vehicle model is required")
    private String model;

    @NotBlank(message = "License plate is required")
    private String licensePlate;

    @NotBlank(message = "Color is required")
    private String color;

    @NotNull(message = "Vehicle category is required")
    private VehicleCategory category;

    @Min(value = 1, message = "Seat capacity must be at least 1")
    @Max(value = 10, message = "Seat capacity cannot exceed 10")
    private int seatCapacity = 4;

    public RegisterVehicleRequest() {
    }

    public RegisterVehicleRequest(String make, String model, String licensePlate, String color, VehicleCategory category, int seatCapacity) {
        this.make = make;
        this.model = model;
        this.licensePlate = licensePlate;
        this.color = color;
        this.category = category;
        this.seatCapacity = seatCapacity;
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
}
