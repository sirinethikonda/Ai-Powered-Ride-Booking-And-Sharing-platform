package com.rideapp.payment.gateway;

import com.rideapp.resilience.exception.PaymentNetworkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class MockPaymentGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(MockPaymentGateway.class);

    public enum SimulationMode {
        SUCCESS,
        DECLINED,
        NETWORK_ERROR,
        TIMEOUT,
        UNCERTAIN
    }

    private final AtomicReference<SimulationMode> currentMode = new AtomicReference<>(SimulationMode.SUCCESS);

    public void setSimulationMode(SimulationMode mode) {
        log.info("MockPaymentGateway simulation mode set to: {}", mode);
        this.currentMode.set(mode);
    }

    public SimulationMode getSimulationMode() {
        return this.currentMode.get();
    }

    @Override
    public PaymentGatewayResponse charge(PaymentGatewayRequest request) {
        SimulationMode mode = currentMode.get();
        log.info("Processing mock payment charge for ride {} with mode {}", request.rideId(), mode);

        switch (mode) {
            case DECLINED:
                return PaymentGatewayResponse.declined("INSUFFICIENT_FUNDS", "Card balance insufficient to cover ride fare");

            case NETWORK_ERROR:
                throw new PaymentNetworkException("Payment gateway socket timeout or connection reset");

            case TIMEOUT:
                try {
                    Thread.sleep(3500); // Exceeds 3000ms TimeLimiter
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new PaymentNetworkException("Payment processing interrupted", e);
                }
                return PaymentGatewayResponse.success("TX-SLOW-" + UUID.randomUUID());

            case UNCERTAIN:
                return PaymentGatewayResponse.pending("TX-UNCERTAIN-" + UUID.randomUUID());

            case SUCCESS:
            default:
                return PaymentGatewayResponse.success("TX-OK-" + UUID.randomUUID());
        }
    }

    @Override
    public PaymentGatewayResponse queryStatus(String transactionReference) {
        log.info("Querying status for gateway reference: {}", transactionReference);
        return PaymentGatewayResponse.success(transactionReference);
    }

    @Override
    public PaymentGatewayResponse refund(String transactionReference, BigDecimal amount) {
        log.info("Processing refund for reference: {}, amount: {}", transactionReference, amount);
        return PaymentGatewayResponse.success("REFUND-" + UUID.randomUUID());
    }
}
