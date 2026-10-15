package com.rideapp.payment;

import com.rideapp.booking.entity.Ride;
import com.rideapp.booking.repository.RideRepository;
import com.rideapp.messaging.outbox.OutboxService;
import com.rideapp.payment.entity.Payment;
import com.rideapp.payment.entity.PaymentStatus;
import com.rideapp.payment.gateway.MockPaymentGateway;
import com.rideapp.payment.gateway.PaymentGatewayRequest;
import com.rideapp.payment.gateway.PaymentGatewayResponse;
import com.rideapp.payment.repository.PaymentRepository;
import com.rideapp.payment.repository.PaymentTransactionRepository;
import com.rideapp.payment.service.PaymentService;
import com.rideapp.pricing.entity.FareBreakdown;
import com.rideapp.resilience.exception.PaymentNetworkException;
import com.rideapp.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResiliencePaymentCircuitBreakerTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private RideRepository rideRepository;

    @Mock
    private OutboxService outboxService;

    private MockPaymentGateway mockPaymentGateway;
    private PaymentService paymentService;

    private Ride testRide;
    private User testPassenger;
    private UUID rideId;

    @BeforeEach
    void setUp() {
        mockPaymentGateway = new MockPaymentGateway();
        paymentService = new PaymentService(
                paymentRepository,
                paymentTransactionRepository,
                rideRepository,
                mockPaymentGateway,
                outboxService
        );

        rideId = UUID.randomUUID();
        testPassenger = new User();
        testPassenger.setId(UUID.randomUUID());

        testRide = new Ride();
        testRide.setId(rideId);
        testRide.setPassenger(testPassenger);

        FareBreakdown fare = new FareBreakdown();
        fare.setFinalFare(new BigDecimal("150.00"));
        testRide.setFareBreakdown(fare);

        lenient().when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(UUID.randomUUID());
            }
            return p;
        });
    }

    @Test
    @DisplayName("Successful payment transitions to SUCCEEDED and creates audit transaction")
    void testSuccessfulPayment() {
        mockPaymentGateway.setSimulationMode(MockPaymentGateway.SimulationMode.SUCCESS);

        when(paymentRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(rideRepository.findById(rideId)).thenReturn(Optional.of(testRide));

        Payment result = paymentService.processRidePayment(rideId, "IDEM-TEST-1");

        assertNotNull(result);
        assertEquals(PaymentStatus.SUCCEEDED, result.getStatus());
        verify(paymentTransactionRepository, times(1)).save(any());
        verify(outboxService, times(1)).recordEvent(eq("Payment"), any(), eq("PaymentSucceededEvent"), any(), any(), any());
    }

    @Test
    @DisplayName("Declined payment transitions to FAILED without throwing unhandled exceptions")
    void testDeclinedPayment() {
        mockPaymentGateway.setSimulationMode(MockPaymentGateway.SimulationMode.DECLINED);

        when(paymentRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(rideRepository.findById(rideId)).thenReturn(Optional.of(testRide));

        Payment result = paymentService.processRidePayment(rideId, "IDEM-TEST-2");

        assertNotNull(result);
        assertEquals(PaymentStatus.FAILED, result.getStatus());
        verify(paymentTransactionRepository, times(1)).save(any());
        verify(outboxService, times(1)).recordEvent(eq("Payment"), any(), eq("PaymentFailedEvent"), any(), any(), any());
    }

    @Test
    @DisplayName("Uncertain/timeout outcome transitions to RECONCILIATION_REQUIRED to prevent double charge")
    void testUncertainOutcomeRequiresReconciliation() {
        mockPaymentGateway.setSimulationMode(MockPaymentGateway.SimulationMode.UNCERTAIN);

        when(paymentRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(rideRepository.findById(rideId)).thenReturn(Optional.of(testRide));

        Payment result = paymentService.processRidePayment(rideId, "IDEM-TEST-3");

        assertNotNull(result);
        assertEquals(PaymentStatus.RECONCILIATION_REQUIRED, result.getStatus());
        verify(paymentTransactionRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Mock gateway directly simulates socket network error")
    void testDirectNetworkErrorSimulation() {
        mockPaymentGateway.setSimulationMode(MockPaymentGateway.SimulationMode.NETWORK_ERROR);

        PaymentGatewayRequest req = new PaymentGatewayRequest(
                UUID.randomUUID(), rideId, testPassenger.getId(), new BigDecimal("150.00"), "INR", "KEY"
        );

        assertThrows(PaymentNetworkException.class, () -> mockPaymentGateway.charge(req));
    }
}
