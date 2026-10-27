package com.smarkpark.smartparktest.common;

import com.smarkpark.smartparktest.lot.ParkingLot;
import com.smarkpark.smartparktest.lot.ParkingLotRepository;
import com.smarkpark.smartparktest.vehicle.Vehicle;
import com.smarkpark.smartparktest.vehicle.VehicleRepository;
import com.smarkpark.smartparktest.vehicle.VehicleType;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;

@Component
@ConditionalOnProperty(name = "smartpark.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DemoData implements ApplicationRunner {
    private final ParkingLotRepository lots;
    private final VehicleRepository vehicles;

    public DemoData(ParkingLotRepository lots, VehicleRepository vehicles) {
        this.lots = lots;
        this.vehicles = vehicles;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        lots.saveAll(Arrays.asList(new ParkingLot("CENTRAL", "Central Avenue", 10, new BigDecimal("0.50")),
                new ParkingLot("RIVERSIDE", "Riverside Road", 5, new BigDecimal("1.00"))));
        vehicles.saveAll(Arrays.asList(new Vehicle("CAR-001", VehicleType.CAR, "Jane Doe"),
                new Vehicle("BIKE-001", VehicleType.MOTORCYCLE, "Alex Lee"),
                new Vehicle("TRUCK-001", VehicleType.TRUCK, "Sam Tan")));
    }
}

