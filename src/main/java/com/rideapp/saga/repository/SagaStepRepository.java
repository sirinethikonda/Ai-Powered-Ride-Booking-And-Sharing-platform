package com.rideapp.saga.repository;

import com.rideapp.saga.entity.SagaStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SagaStepRepository extends JpaRepository<SagaStep, UUID> {
    List<SagaStep> findBySagaInstanceIdOrderByStartedAtAsc(UUID sagaInstanceId);
}
