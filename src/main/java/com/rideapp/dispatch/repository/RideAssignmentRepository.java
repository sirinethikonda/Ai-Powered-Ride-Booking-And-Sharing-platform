package com.rideapp.dispatch.repository;

import com.rideapp.dispatch.entity.AssignmentStatus;
import com.rideapp.dispatch.entity.RideAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RideAssignmentRepository extends JpaRepository<RideAssignment, UUID> {
    Optional<RideAssignment> findByRideIdAndStatus(UUID rideId, AssignmentStatus status);
    List<RideAssignment> findByRideId(UUID rideId);
    List<RideAssignment> findByDriverIdAndStatus(UUID driverId, AssignmentStatus status);
}
