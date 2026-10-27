package com.smarkpark.smartparktest.parking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ParkingSessionRepository extends JpaRepository<ParkingSession, UUID> {
}

