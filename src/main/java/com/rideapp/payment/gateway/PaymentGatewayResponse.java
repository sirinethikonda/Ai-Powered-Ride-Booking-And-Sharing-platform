package com.rideapp.payment.gateway;

public record PaymentGatewayResponse(
        boolean successful,
        String transactionReference,
        String status,
        String errorCode,
        String errorMessage
) {
    public static PaymentGatewayResponse success(String txRef) {
        return new PaymentGatewayResponse(true, txRef, "SUCCESS", null, null);
    }

    public static PaymentGatewayResponse declined(String code, String msg) {
        return new PaymentGatewayResponse(false, null, "DECLINED", code, msg);
    }

    public static PaymentGatewayResponse pending(String txRef) {
        return new PaymentGatewayResponse(false, txRef, "PENDING", null, "Payment pending gateway settlement");
    }
}
