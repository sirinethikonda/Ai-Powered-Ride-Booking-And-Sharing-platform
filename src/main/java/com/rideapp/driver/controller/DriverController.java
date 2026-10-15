package com.rideapp.driver.controller;

import com.rideapp.common.dto.ApiResponse;
import com.rideapp.common.security.SecurityUtils;
import com.rideapp.driver.dto.DriverProfileResponse;
import com.rideapp.driver.dto.RegisterVehicleRequest;
import com.rideapp.driver.dto.UpdateAvailabilityRequest;
import com.rideapp.driver.dto.VehicleResponse;
import com.rideapp.driver.service.DriverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/drivers")
@Tag(name = "Driver Management", description = "Driver availability, vehicle registration, and status")
public class DriverController {

    private final DriverService driverService;
    private final SecurityUtils securityUtils;

    public DriverController(DriverService driverService, SecurityUtils securityUtils) {
        this.driverService = driverService;
        this.securityUtils = securityUtils;
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Get current driver profile", description = "Retrieves operational details and vehicles of authenticated driver")
    public ResponseEntity<ApiResponse<DriverProfileResponse>> getMyProfile() {
        UUID driverId = securityUtils.getCurrentUserId();
        DriverProfileResponse profile = driverService.getDriverProfile(driverId);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PutMapping("/me/availability")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Update driver operational availability", description = "Toggles driver availability between AVAILABLE and OFFLINE with vehicle eligibility validation")
    public ResponseEntity<ApiResponse<DriverProfileResponse>> updateAvailability(
            @Valid @RequestBody UpdateAvailabilityRequest request) {
        UUID driverId = securityUtils.getCurrentUserId();
        DriverProfileResponse response = driverService.updateAvailability(driverId, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success("Availability updated successfully", response));
    }

    @PostMapping("/me/vehicle")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Register vehicle for driver", description = "Registers vehicle with seat capacity and category")
    public ResponseEntity<ApiResponse<VehicleResponse>> registerVehicle(
            @Valid @RequestBody RegisterVehicleRequest request) {
        UUID driverId = securityUtils.getCurrentUserId();
        VehicleResponse response = driverService.registerVehicle(driverId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Vehicle registered successfully", response));
    }
}
