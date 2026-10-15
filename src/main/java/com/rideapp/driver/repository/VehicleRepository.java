package com.rideapp.driver.repository;

import com.rideapp.driver.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {
    List<Vehicle> findByDriverIdAndIsActiveTrue(UUID driverId);
    Optional<Vehicle> findByLicensePlate(String licensePlate);
}
