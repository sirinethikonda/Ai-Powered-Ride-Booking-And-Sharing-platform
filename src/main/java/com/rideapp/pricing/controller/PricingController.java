package com.rideapp.pricing.controller;

import com.rideapp.common.dto.ApiResponse;
import com.rideapp.pricing.dto.RideQuoteRequest;
import com.rideapp.pricing.dto.RideQuoteResponse;
import com.rideapp.pricing.service.PricingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ride-quotes")
@Tag(name = "Ride Pricing & Quotes", description = "Fare estimation for solo and conditional shared rides")
public class PricingController {

    private final PricingService pricingService;

    public PricingController(PricingService pricingService) {
        this.pricingService = pricingService;
    }

    @PostMapping
    @Operation(summary = "Get ride fare estimate", description = "Calculates distance, duration, solo fare, and conditional shared fare")
    public ResponseEntity<ApiResponse<RideQuoteResponse>> getQuote(@Valid @RequestBody RideQuoteRequest request) {
        RideQuoteResponse response = pricingService.generateQuote(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
