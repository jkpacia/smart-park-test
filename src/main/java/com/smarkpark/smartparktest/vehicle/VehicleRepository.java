package com.smarkpark.smartparktest.vehicle;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Vehicle v where v.licensePlate=:plate")
    Optional<Vehicle> findLocked(@Param("plate") String plate);
}

