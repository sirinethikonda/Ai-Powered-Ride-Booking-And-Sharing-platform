package com.rideapp.dispatch.dto;

public class DriverOfferDecisionRequest {

    private String reason;

    public DriverOfferDecisionRequest() {
    }

    public DriverOfferDecisionRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
