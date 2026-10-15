package com.rideapp.payment.gateway;

import java.math.BigDecimal;

public interface PaymentGateway {
    PaymentGatewayResponse charge(PaymentGatewayRequest request);
    PaymentGatewayResponse queryStatus(String transactionReference);
    PaymentGatewayResponse refund(String transactionReference, BigDecimal amount);
}
