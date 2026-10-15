package com.rideapp.booking.dto;

import jakarta.validation.constraints.NotBlank;

public class CancelRideRequest {

    @NotBlank(message = "Cancellation reason is required")
    private String reason;

    public CancelRideRequest() {
    }

    public CancelRideRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
