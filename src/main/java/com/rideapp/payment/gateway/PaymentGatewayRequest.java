package com.rideapp.payment.gateway;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentGatewayRequest(
        UUID paymentId,
        UUID rideId,
        UUID passengerId,
        BigDecimal amount,
        String currency,
        String idempotencyKey
) {}
