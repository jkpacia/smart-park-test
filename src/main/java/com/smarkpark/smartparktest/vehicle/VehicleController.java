package com.smarkpark.smartparktest.vehicle;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {
    private final VehicleService service;

    public VehicleController(VehicleService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VehicleResponse register(@Valid @RequestBody RegisterVehicle r) {
        return service.register(r);
    }

    public static class RegisterVehicle {
        @NotBlank
        @Pattern(regexp = "[A-Z0-9-]+")
        private String licensePlate;
        @NotNull
        private VehicleType type;
        @NotBlank
        @Pattern(regexp = "[A-Za-z ]+")
        private String ownerName;

        public RegisterVehicle() {
        }

        public String getLicensePlate() {
            return licensePlate;
        }

        public void setLicensePlate(String v) {
            licensePlate = LicensePlates.normalize(v);
        }

        public VehicleType getType() {
            return type;
        }

        public void setType(VehicleType v) {
            type = v;
        }

        public String getOwnerName() {
            return ownerName;
        }

        public void setOwnerName(String v) {
            ownerName = v == null ? null : v.trim();
        }
    }

    public static class VehicleResponse {
        private final String licensePlate, ownerName;
        private final VehicleType type;

        public VehicleResponse(String licensePlate, VehicleType type, String ownerName) {
            this.licensePlate = licensePlate;
            this.type = type;
            this.ownerName = ownerName;
        }

        public String getLicensePlate() {
            return licensePlate;
        }

        public VehicleType getType() {
            return type;
        }

        public String getOwnerName() {
            return ownerName;
        }
    }
}

