package com.smarkpark.smartparktest.vehicle;

import com.smarkpark.smartparktest.common.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VehicleService {
    private final VehicleRepository vehicles;

    public VehicleService(VehicleRepository vehicles) {
        this.vehicles = vehicles;
    }

    @Transactional
    public VehicleController.VehicleResponse register(VehicleController.RegisterVehicle r) {
        if (vehicles.existsById(r.getLicensePlate()))
            throw ApiException.conflict("DUPLICATE_RESOURCE", "License plate already exists");
        Vehicle v = vehicles.saveAndFlush(new Vehicle(r.getLicensePlate(), r.getType(), r.getOwnerName()));
        return new VehicleController.VehicleResponse(v.getLicensePlate(), v.getType(), v.getOwnerName());
    }
}

