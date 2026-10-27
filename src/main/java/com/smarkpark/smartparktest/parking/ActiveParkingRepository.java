package com.smarkpark.smartparktest.parking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ActiveParkingRepository extends JpaRepository<ActiveParking,String> {
    long countByLot_LotId(String lotId);
    @Query("select a from ActiveParking a join fetch a.session s join fetch s.vehicle where a.lot.lotId=:id order by s.checkedInAt,a.licensePlate")
    List<ActiveParking> findParked(@Param("id") String lotId);
    @Query("select a.licensePlate from ActiveParking a where a.session.checkedInAt < :cutoff order by a.licensePlate")
    List<String> findOverduePlates(@Param("cutoff") Instant cutoff);
}

