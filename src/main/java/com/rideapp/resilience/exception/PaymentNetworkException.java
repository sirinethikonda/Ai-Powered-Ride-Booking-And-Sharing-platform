package com.rideapp.resilience.exception;

public class PaymentNetworkException extends RuntimeException {
    public PaymentNetworkException(String message) {
        super(message);
    }

    public PaymentNetworkException(String message, Throwable cause) {
        super(message, cause);
    }
}
