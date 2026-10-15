package com.rideapp.location.controller;

import com.rideapp.common.dto.ApiResponse;
import com.rideapp.common.security.SecurityUtils;
import com.rideapp.location.dto.DriverLocationUpdateRequest;
import com.rideapp.location.dto.NearbyDriverDto;
import com.rideapp.location.service.DriverLocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/drivers")
@Tag(name = "Driver Location", description = "Driver real-time geospatial location ingestion and nearby discovery")
public class DriverLocationController {

    private final DriverLocationService driverLocationService;
    private final SecurityUtils securityUtils;

    public DriverLocationController(DriverLocationService driverLocationService, SecurityUtils securityUtils) {
        this.driverLocationService = driverLocationService;
        this.securityUtils = securityUtils;
    }

    @PutMapping("/me/location")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Update driver real-time location", description = "Updates driver coordinates in Redis GEO and PostGIS spatial storage")
    public ResponseEntity<ApiResponse<String>> updateLocation(@Valid @RequestBody DriverLocationUpdateRequest request) {
        UUID driverId = securityUtils.getCurrentUserId();
        driverLocationService.updateDriverLocation(driverId, request);
        return ResponseEntity.ok(ApiResponse.success("Location updated successfully", "OK"));
    }

    @GetMapping("/nearby")
    @Operation(summary = "Discover nearby available drivers", description = "Finds available drivers within radius using Redis GEO / PostGIS")
    public ResponseEntity<ApiResponse<List<NearbyDriverDto>>> getNearbyDrivers(
            @RequestParam double longitude,
            @RequestParam double latitude,
            @RequestParam(defaultValue = "5.0") double radiusKm,
            @RequestParam(defaultValue = "10") int limit) {
        List<NearbyDriverDto> drivers = driverLocationService.findNearbyAvailableDrivers(longitude, latitude, radiusKm, limit);
        return ResponseEntity.ok(ApiResponse.success(drivers));
    }
}
