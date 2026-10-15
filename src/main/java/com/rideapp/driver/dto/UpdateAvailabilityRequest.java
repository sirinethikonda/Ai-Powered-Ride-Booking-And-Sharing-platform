package com.rideapp.driver.dto;

import com.rideapp.driver.entity.OperationalStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateAvailabilityRequest {

    @NotNull(message = "Operational status is required")
    private OperationalStatus status;

    public UpdateAvailabilityRequest() {
    }

    public UpdateAvailabilityRequest(OperationalStatus status) {
        this.status = status;
    }

    public OperationalStatus getStatus() {
        return status;
    }

    public void setStatus(OperationalStatus status) {
        this.status = status;
    }
}
