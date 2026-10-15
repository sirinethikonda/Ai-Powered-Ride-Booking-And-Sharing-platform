package com.rideapp.driver.service;

import com.rideapp.common.exception.BusinessRuleException;
import com.rideapp.common.exception.ConflictException;
import com.rideapp.common.exception.InvalidStateTransitionException;
import com.rideapp.common.exception.ResourceNotFoundException;
import com.rideapp.driver.dto.DriverProfileResponse;
import com.rideapp.driver.dto.RegisterVehicleRequest;
import com.rideapp.driver.dto.VehicleResponse;
import com.rideapp.driver.entity.Driver;
import com.rideapp.driver.entity.OperationalStatus;
import com.rideapp.driver.entity.Vehicle;
import com.rideapp.driver.repository.DriverRepository;
import com.rideapp.driver.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class DriverService {

    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;

    public DriverService(DriverRepository driverRepository, VehicleRepository vehicleRepository) {
        this.driverRepository = driverRepository;
        this.vehicleRepository = vehicleRepository;
    }

    @Transactional(readOnly = true)
    public DriverProfileResponse getDriverProfile(UUID driverId) {
        Driver driver = findDriverById(driverId);
        return DriverProfileResponse.fromEntity(driver);
    }

    @Transactional
    public VehicleResponse registerVehicle(UUID driverId, RegisterVehicleRequest request) {
        Driver driver = findDriverById(driverId);

        if (vehicleRepository.findByLicensePlate(request.getLicensePlate()).isPresent()) {
            throw new ConflictException("Vehicle with license plate already exists: " + request.getLicensePlate());
        }

        Vehicle vehicle = new Vehicle(
                driver,
                request.getMake(),
                request.getModel(),
                request.getLicensePlate(),
                request.getColor(),
                request.getCategory(),
                request.getSeatCapacity()
        );

        vehicle = vehicleRepository.save(vehicle);
        return VehicleResponse.fromEntity(vehicle);
    }

    @Transactional
    public DriverProfileResponse updateAvailability(UUID driverId, OperationalStatus targetStatus) {
        Driver driver = findDriverById(driverId);

        if (driver.getOperationalStatus() == OperationalStatus.ON_TRIP) {
            throw new InvalidStateTransitionException("Driver is currently on an active trip and cannot change operational status");
        }

        if (driver.getOperationalStatus() == OperationalStatus.SUSPENDED) {
            throw new BusinessRuleException("Suspended driver account cannot be marked as available");
        }

        if (targetStatus == OperationalStatus.AVAILABLE) {
            List<Vehicle> activeVehicles = vehicleRepository.findByDriverIdAndIsActiveTrue(driverId);
            if (activeVehicles.isEmpty()) {
                throw new BusinessRuleException("Cannot set status to AVAILABLE without an active registered vehicle");
            }
        }

        driver.setOperationalStatus(targetStatus);
        driver = driverRepository.save(driver);
        return DriverProfileResponse.fromEntity(driver);
    }

    public Driver findDriverById(UUID driverId) {
        return driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver", driverId));
    }
}
