package com.rideapp.saga.controller;

import com.rideapp.common.dto.ApiResponse;
import com.rideapp.saga.entity.SagaInstance;
import com.rideapp.saga.service.SagaCoordinatorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sagas")
@Tag(name = "Saga Orchestration", description = "Distributed transaction coordination and workflow inspection")
public class SagaController {

    private final SagaCoordinatorService sagaCoordinatorService;

    public SagaController(SagaCoordinatorService sagaCoordinatorService) {
        this.sagaCoordinatorService = sagaCoordinatorService;
    }

    @PostMapping("/trip-completion/{rideId}")
    @Operation(summary = "Execute trip completion and payment saga", description = "Coordinates multi-step verification and payment settlement")
    public ResponseEntity<ApiResponse<SagaInstance>> triggerTripCompletionSaga(@PathVariable UUID rideId) {
        SagaInstance saga = sagaCoordinatorService.executeTripCompletionAndPaymentSaga(rideId);
        return ResponseEntity.ok(ApiResponse.success("Saga executed", saga));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get saga workflow details", description = "Inspects saga status and step-by-step progress")
    public ResponseEntity<ApiResponse<SagaInstance>> getSaga(@PathVariable UUID id) {
        SagaInstance saga = sagaCoordinatorService.getSagaDetails(id);
        return ResponseEntity.ok(ApiResponse.success(saga));
    }
}
