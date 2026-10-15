package com.rideapp.sharedride.repository;

import com.rideapp.sharedride.entity.SharedGroupStatus;
import com.rideapp.sharedride.entity.SharedRideGroup;
import com.rideapp.user.entity.Gender;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SharedRideGroupRepository extends JpaRepository<SharedRideGroup, UUID> {
    List<SharedRideGroup> findByStatusAndAllowedGender(SharedGroupStatus status, Gender allowedGender);
}
