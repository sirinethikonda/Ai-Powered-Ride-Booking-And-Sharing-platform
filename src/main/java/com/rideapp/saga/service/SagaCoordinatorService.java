package com.rideapp.saga.service;

import com.rideapp.booking.entity.Ride;
import com.rideapp.booking.repository.RideRepository;
import com.rideapp.common.exception.ResourceNotFoundException;
import com.rideapp.payment.entity.Payment;
import com.rideapp.payment.entity.PaymentStatus;
import com.rideapp.payment.service.PaymentService;
import com.rideapp.saga.entity.*;
import com.rideapp.saga.repository.SagaInstanceRepository;
import com.rideapp.saga.repository.SagaStepRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class SagaCoordinatorService {

    private static final Logger log = LoggerFactory.getLogger(SagaCoordinatorService.class);

    private final SagaInstanceRepository sagaInstanceRepository;
    private final SagaStepRepository sagaStepRepository;
    private final RideRepository rideRepository;
    private final PaymentService paymentService;

    public SagaCoordinatorService(
            SagaInstanceRepository sagaInstanceRepository,
            SagaStepRepository sagaStepRepository,
            RideRepository rideRepository,
            PaymentService paymentService) {
        this.sagaInstanceRepository = sagaInstanceRepository;
        this.sagaStepRepository = sagaStepRepository;
        this.rideRepository = rideRepository;
        this.paymentService = paymentService;
    }

    @Transactional
    public SagaInstance executeTripCompletionAndPaymentSaga(UUID rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride", rideId));

        SagaInstance saga = new SagaInstance(SagaType.TRIP_COMPLETION_PAYMENT, rideId, null);
        saga.setStatus(SagaStatus.IN_PROGRESS);
        saga = sagaInstanceRepository.save(saga);

        // Step 1: Verify ride state
        SagaStep step1 = new SagaStep(saga, "VERIFY_TRIP_ELIGIBILITY");
        sagaStepRepository.save(step1);
        try {
            if (ride.getStatus().isTerminal() && ride.getStatus() != com.rideapp.booking.entity.RideStatus.COMPLETED) {
                throw new IllegalStateException("Ride cannot be completed in current status: " + ride.getStatus());
            }
            step1.markSuccess();
            sagaStepRepository.save(step1);
        } catch (Exception ex) {
            step1.markFailed(ex.getMessage());
            sagaStepRepository.save(step1);
            saga.setStatus(SagaStatus.FAILED);
            return sagaInstanceRepository.save(saga);
        }

        // Step 2: Settle Payment
        SagaStep step2 = new SagaStep(saga, "EXECUTE_PAYMENT");
        sagaStepRepository.save(step2);
        try {
            String paymentKey = "SAGA-PAY-" + rideId;
            Payment payment = paymentService.processRidePayment(rideId, paymentKey);

            if (payment.getStatus() == PaymentStatus.SUCCEEDED) {
                step2.markSuccess();
                saga.setStatus(SagaStatus.COMPLETED);
            } else if (payment.getStatus() == PaymentStatus.RECONCILIATION_REQUIRED) {
                step2.markSuccess(); // Awaiting async reconciliation
                saga.setStatus(SagaStatus.IN_PROGRESS);
            } else {
                step2.markFailed("Payment was declined or failed: " + payment.getStatus());
                saga.setStatus(SagaStatus.COMPENSATING);
                // Note: We don't rollback the physical transit of the passenger, but log compensation requirement
                step2.markCompensated();
                saga.setStatus(SagaStatus.FAILED);
            }
            sagaStepRepository.save(step2);
        } catch (Exception ex) {
            step2.markFailed(ex.getMessage());
            sagaStepRepository.save(step2);
            saga.setStatus(SagaStatus.FAILED);
        }

        return sagaInstanceRepository.save(saga);
    }

    @Transactional(readOnly = true)
    public SagaInstance getSagaDetails(UUID sagaId) {
        return sagaInstanceRepository.findById(sagaId)
                .orElseThrow(() -> new ResourceNotFoundException("SagaInstance", sagaId));
    }
}
