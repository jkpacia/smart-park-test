package com.smarkpark.smartparktest.parking;

import com.smarkpark.smartparktest.vehicle.LicensePlates;
import com.smarkpark.smartparktest.vehicle.VehicleType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/lots/{lotId}")
public class ParkingController {
    private final ParkingService service;

    public ParkingController(ParkingService service) {
        this.service = service;
    }

    @PostMapping("/check-ins")
    @ResponseStatus(HttpStatus.CREATED)
    public Receipt checkIn(@PathVariable String lotId, @Valid @RequestBody ParkingRequest r) {
        return service.checkIn(lotId, r.getLicensePlate());
    }

    @PostMapping("/check-outs")
    public Receipt checkOut(@PathVariable String lotId, @Valid @RequestBody ParkingRequest r) {
        return service.checkOut(lotId, r.getLicensePlate());
    }

    @GetMapping("/occupancy")
    public Occupancy occupancy(@PathVariable String lotId) {
        return service.occupancy(lotId);
    }

    @GetMapping("/vehicles")
    public List<ParkedVehicle> vehicles(@PathVariable String lotId) {
        return service.parkedVehicles(lotId);
    }

    public static class ParkingRequest {
        @NotBlank
        @Pattern(regexp = "[A-Z0-9-]+")
        private String licensePlate;

        public ParkingRequest() {
        }

        public String getLicensePlate() {
            return licensePlate;
        }

        public void setLicensePlate(String v) {
            licensePlate = LicensePlates.normalize(v);
        }
    }

    public static class Occupancy {
        private final String lotId;
        private final int capacity;
        private final long occupiedSpaces, availableSpaces;

        public Occupancy(String lotId, int capacity, long occupiedSpaces, long availableSpaces) {
            this.lotId = lotId;
            this.capacity = capacity;
            this.occupiedSpaces = occupiedSpaces;
            this.availableSpaces = availableSpaces;
        }

        public String getLotId() {
            return lotId;
        }

        public int getCapacity() {
            return capacity;
        }

        public long getOccupiedSpaces() {
            return occupiedSpaces;
        }

        public long getAvailableSpaces() {
            return availableSpaces;
        }
    }

    public static class ParkedVehicle {
        private final String licensePlate, ownerName;
        private final VehicleType type;
        private final Instant checkedInAt;

        public ParkedVehicle(String plate, VehicleType type, String owner, Instant entry) {
            licensePlate = plate;
            this.type = type;
            ownerName = owner;
            checkedInAt = entry;
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

        public Instant getCheckedInAt() {
            return checkedInAt;
        }
    }

    public static class Receipt {
        private final UUID sessionId;
        private final String licensePlate, lotId;
        private final SessionStatus status;
        private final Instant checkedInAt, checkedOutAt;
        private final long elapsedSeconds, billedMinutes;
        private final BigDecimal costPerMinute, totalCost;

        public Receipt(UUID sessionId, String licensePlate, String lotId, SessionStatus status, Instant checkedInAt, Instant checkedOutAt,
                       long elapsedSeconds, long billedMinutes, BigDecimal costPerMinute, BigDecimal totalCost) {
            this.sessionId = sessionId;
            this.licensePlate = licensePlate;
            this.lotId = lotId;
            this.status = status;
            this.checkedInAt = checkedInAt;
            this.checkedOutAt = checkedOutAt;
            this.elapsedSeconds = elapsedSeconds;
            this.billedMinutes = billedMinutes;
            this.costPerMinute = costPerMinute;
            this.totalCost = totalCost;
        }

        public UUID getSessionId() {
            return sessionId;
        }

        public String getLicensePlate() {
            return licensePlate;
        }

        public String getLotId() {
            return lotId;
        }

        public SessionStatus getStatus() {
            return status;
        }

        public Instant getCheckedInAt() {
            return checkedInAt;
        }

        public Instant getCheckedOutAt() {
            return checkedOutAt;
        }

        public long getElapsedSeconds() {
            return elapsedSeconds;
        }

        public long getBilledMinutes() {
            return billedMinutes;
        }

        public BigDecimal getCostPerMinute() {
            return costPerMinute;
        }

        public BigDecimal getTotalCost() {
            return totalCost;
        }
    }
}

