package com.rideapp.saga.repository;

import com.rideapp.saga.entity.SagaInstance;
import com.rideapp.saga.entity.SagaStatus;
import com.rideapp.saga.entity.SagaType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SagaInstanceRepository extends JpaRepository<SagaInstance, UUID> {
    Optional<SagaInstance> findBySagaTypeAndRelatedEntityId(SagaType sagaType, UUID relatedEntityId);
    List<SagaInstance> findByStatus(SagaStatus status);
}
