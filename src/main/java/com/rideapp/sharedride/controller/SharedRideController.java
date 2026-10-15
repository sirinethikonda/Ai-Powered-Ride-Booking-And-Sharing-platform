package com.rideapp.sharedride.controller;

import com.rideapp.common.dto.ApiResponse;
import com.rideapp.sharedride.entity.SharedRideGroup;
import com.rideapp.sharedride.service.SharedRideService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shared-rides")
@Tag(name = "Shared Rides", description = "Gender-compatible shared rides and matching groups")
public class SharedRideController {

    private final SharedRideService sharedRideService;

    public SharedRideController(SharedRideService sharedRideService) {
        this.sharedRideService = sharedRideService;
    }

    @PostMapping("/match/{rideId}")
    @Operation(summary = "Match or queue shared ride", description = "Evaluates gender compatibility, route overlap, and applies conditional discounts")
    public ResponseEntity<ApiResponse<SharedRideGroup>> matchRide(@PathVariable UUID rideId) {
        SharedRideGroup group = sharedRideService.matchOrEnqueueSharedRide(rideId);
        return ResponseEntity.ok(ApiResponse.success("Shared ride processed", group));
    }

    @GetMapping("/groups/{id}")
    @Operation(summary = "Get shared group details", description = "Retrieves group status and passenger details")
    public ResponseEntity<ApiResponse<SharedRideGroup>> getGroup(@PathVariable UUID id) {
        SharedRideGroup group = sharedRideService.getGroupDetails(id);
        return ResponseEntity.ok(ApiResponse.success(group));
    }
}
