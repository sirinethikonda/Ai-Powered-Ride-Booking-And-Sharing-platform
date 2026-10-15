package com.rideapp.payment.controller;

import com.rideapp.common.dto.ApiResponse;
import com.rideapp.payment.entity.Payment;
import com.rideapp.payment.gateway.MockPaymentGateway;
import com.rideapp.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments & Resilience", description = "Resilient payment processing, refunds, and mock gateway simulation")
public class PaymentController {

    private final PaymentService paymentService;
    private final MockPaymentGateway mockPaymentGateway;

    public PaymentController(PaymentService paymentService, MockPaymentGateway mockPaymentGateway) {
        this.paymentService = paymentService;
        this.mockPaymentGateway = mockPaymentGateway;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get payment details", description = "Retrieves payment status, currency, and transaction history")
    public ResponseEntity<ApiResponse<Payment>> getPayment(@PathVariable UUID id) {
        Payment payment = paymentService.getPayment(id);
        return ResponseEntity.ok(ApiResponse.success(payment));
    }

    @PostMapping("/{id}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Refund payment", description = "Initiates refund for settled payments")
    public ResponseEntity<ApiResponse<Payment>> refundPayment(
            @PathVariable UUID id,
            @RequestParam BigDecimal amount) {
        Payment refunded = paymentService.refundPayment(id, amount);
        return ResponseEntity.ok(ApiResponse.success("Refund processed successfully", refunded));
    }

    @PostMapping("/mock-mode")
    @Operation(summary = "Set mock gateway simulation mode", description = "Configure gateway behavior: SUCCESS, DECLINED, NETWORK_ERROR, TIMEOUT, UNCERTAIN")
    public ResponseEntity<ApiResponse<String>> setMockSimulationMode(@RequestParam MockPaymentGateway.SimulationMode mode) {
        mockPaymentGateway.setSimulationMode(mode);
        return ResponseEntity.ok(ApiResponse.success("Mock payment gateway mode updated to: " + mode, mode.name()));
    }
}
