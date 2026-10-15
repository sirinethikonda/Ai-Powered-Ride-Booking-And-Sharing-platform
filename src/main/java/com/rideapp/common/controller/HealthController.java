package com.rideapp.common.controller;

import com.rideapp.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Health Check", description = "System health and status inspection")
public class HealthController {

    @GetMapping
    @Operation(summary = "Check application health", description = "Returns operational status and timestamp")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkHealth() {
        return ResponseEntity.ok(ApiResponse.success("System operational", Map.of(
                "status", "UP",
                "service", "AI-Powered Ride Booking Platform",
                "version", "1.0.0-SNAPSHOT"
        )));
    }
}
