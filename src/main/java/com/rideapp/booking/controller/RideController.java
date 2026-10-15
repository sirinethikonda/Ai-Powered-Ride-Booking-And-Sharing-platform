package com.rideapp.booking.controller;

import com.rideapp.booking.dto.CancelRideRequest;
import com.rideapp.booking.dto.CreateRideRequest;
import com.rideapp.booking.dto.RideResponse;
import com.rideapp.booking.service.RideBookingService;
import com.rideapp.common.dto.ApiResponse;
import com.rideapp.common.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rides")
@Tag(name = "Ride Booking & Lifecycle", description = "Ride request, lifecycle management, and cancellations")
public class RideController {

    private final RideBookingService rideBookingService;
    private final SecurityUtils securityUtils;

    public RideController(RideBookingService rideBookingService, SecurityUtils securityUtils) {
        this.rideBookingService = rideBookingService;
        this.securityUtils = securityUtils;
    }

    @PostMapping
    @PreAuthorize("hasRole('PASSENGER')")
    @Operation(summary = "Book a new ride", description = "Creates a ride request idempotently and triggers driver matching")
    public ResponseEntity<ApiResponse<RideResponse>> bookRide(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKeyHeader,
            @Valid @RequestBody CreateRideRequest request) {

        if (idempotencyKeyHeader != null && !idempotencyKeyHeader.isBlank()) {
            request.setIdempotencyKey(idempotencyKeyHeader);
        }

        UUID passengerId = securityUtils.getCurrentUserId();
        RideResponse response = rideBookingService.bookRide(passengerId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Ride requested successfully", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get ride details", description = "Retrieves current status, assigned driver, and fare breakdown")
    public ResponseEntity<ApiResponse<RideResponse>> getRide(@PathVariable UUID id) {
        RideResponse response = rideBookingService.getRideDetails(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @PreAuthorize("hasRole('PASSENGER')")
    @Operation(summary = "Get passenger rides", description = "Retrieves all ride requests created by the authenticated passenger")
    public ResponseEntity<ApiResponse<List<RideResponse>>> getMyRides() {
        UUID passengerId = securityUtils.getCurrentUserId();
        List<RideResponse> rides = rideBookingService.getPassengerRides(passengerId);
        return ResponseEntity.ok(ApiResponse.success(rides));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel ride", description = "Cancels an unstarted ride and releases assigned driver")
    public ResponseEntity<ApiResponse<RideResponse>> cancelRide(
            @PathVariable UUID id,
            @Valid @RequestBody CancelRideRequest request) {
        UUID actorId = securityUtils.getCurrentUserId();
        RideResponse response = rideBookingService.cancelRide(id, actorId, request.getReason());
        return ResponseEntity.ok(ApiResponse.success("Ride cancelled successfully", response));
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Start ride trip", description = "Marks ride IN_PROGRESS by the assigned driver")
    public ResponseEntity<ApiResponse<RideResponse>> startRide(@PathVariable UUID id) {
        UUID driverId = securityUtils.getCurrentUserId();
        RideResponse response = rideBookingService.startRide(id, driverId);
        return ResponseEntity.ok(ApiResponse.success("Trip started successfully", response));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Complete ride trip", description = "Completes trip and finalizes metrics")
    public ResponseEntity<ApiResponse<RideResponse>> completeRide(
            @PathVariable UUID id,
            @RequestParam(required = false) BigDecimal actualDistanceKm,
            @RequestParam(required = false) Integer actualDurationMin) {
        UUID driverId = securityUtils.getCurrentUserId();
        RideResponse response = rideBookingService.completeRide(id, driverId, actualDistanceKm, actualDurationMin);
        return ResponseEntity.ok(ApiResponse.success("Trip completed successfully", response));
    }
}
