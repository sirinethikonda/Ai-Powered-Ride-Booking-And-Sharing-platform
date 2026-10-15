package com.rideapp.dispatch.controller;

import com.rideapp.common.dto.ApiResponse;
import com.rideapp.common.security.SecurityUtils;
import com.rideapp.dispatch.dto.DriverOfferDecisionRequest;
import com.rideapp.dispatch.dto.OfferResponse;
import com.rideapp.dispatch.service.DriverDispatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/driver-offers")
@Tag(name = "Driver Offers & Dispatch", description = "Driver offer acceptance, rejection, and dispatch triggers")
public class DriverOfferController {

    private final DriverDispatchService driverDispatchService;
    private final SecurityUtils securityUtils;

    public DriverOfferController(DriverDispatchService driverDispatchService, SecurityUtils securityUtils) {
        this.driverDispatchService = driverDispatchService;
        this.securityUtils = securityUtils;
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Accept driver offer", description = "Validates offer deadline and atomically assigns driver to the ride")
    public ResponseEntity<ApiResponse<OfferResponse>> acceptOffer(@PathVariable UUID id) {
        UUID driverId = securityUtils.getCurrentUserId();
        OfferResponse response = driverDispatchService.acceptOffer(id, driverId);
        return ResponseEntity.ok(ApiResponse.success("Offer accepted. Driver assigned to trip.", response));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Reject driver offer", description = "Rejects offer and automatically triggers intelligent driver replacement")
    public ResponseEntity<ApiResponse<OfferResponse>> rejectOffer(
            @PathVariable UUID id,
            @RequestBody(required = false) DriverOfferDecisionRequest request) {
        UUID driverId = securityUtils.getCurrentUserId();
        String reason = request != null ? request.getReason() : "Declined by driver";
        OfferResponse response = driverDispatchService.rejectOffer(id, driverId, reason);
        return ResponseEntity.ok(ApiResponse.success("Offer rejected. Replacement initiated.", response));
    }

    @PostMapping("/dispatch/{rideId}")
    @Operation(summary = "Trigger ride dispatch", description = "Discovers nearby eligible drivers and creates driver offer")
    public ResponseEntity<ApiResponse<OfferResponse>> triggerDispatch(@PathVariable UUID rideId) {
        var offerOpt = driverDispatchService.dispatchRide(rideId);
        return offerOpt
                .map(offer -> ResponseEntity.ok(ApiResponse.success("Offer dispatched to driver", offer)))
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.failure("No eligible nearby drivers found")));
    }
}
