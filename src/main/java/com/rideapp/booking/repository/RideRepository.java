package com.rideapp.booking.repository;

import com.rideapp.booking.entity.Ride;
import com.rideapp.booking.entity.RideStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RideRepository extends JpaRepository<Ride, UUID> {
    Optional<Ride> findByIdempotencyKey(String idempotencyKey);

    List<Ride> findByPassengerIdOrderByCreatedAtDesc(UUID passengerId);

    List<Ride> findByDriverIdAndStatusIn(UUID driverId, List<RideStatus> statuses);

    @Query("SELECT r FROM Ride r WHERE r.status = :status ORDER BY r.createdAt ASC")
    List<Ride> findByStatus(@Param("status") RideStatus status);
}
