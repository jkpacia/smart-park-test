package com.smarkpark.smartparktest.lot;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/lots")
public class LotController {
    private final LotService service;

    public LotController(LotService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LotResponse register(@Valid @RequestBody RegisterLot request) {
        return service.register(request);
    }

    public static class RegisterLot {
        @NotBlank
        @Size(max = 50)
        @Pattern(regexp = "[A-Za-z0-9_-]+")
        private String lotId;
        @NotBlank
        private String location;
        @NotNull
        @Positive
        private Integer capacity;
        @NotNull
        @DecimalMin("0.00")
        @Digits(integer = 17, fraction = 2)
        private BigDecimal costPerMinute;

        public RegisterLot() {
        }

        public String getLotId() {
            return lotId;
        }

        public void setLotId(String v) {
            lotId = v;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String v) {
            location = v;
        }

        public Integer getCapacity() {
            return capacity;
        }

        public void setCapacity(Integer v) {
            capacity = v;
        }

        public BigDecimal getCostPerMinute() {
            return costPerMinute;
        }

        public void setCostPerMinute(BigDecimal v) {
            costPerMinute = v;
        }
    }

    public static class LotResponse {
        private final String lotId, location;
        private final int capacity;
        private final long occupiedSpaces, availableSpaces;
        private final BigDecimal costPerMinute;

        public LotResponse(String lotId, String location, int capacity, long occupiedSpaces, long availableSpaces, BigDecimal costPerMinute) {
            this.lotId = lotId;
            this.location = location;
            this.capacity = capacity;
            this.occupiedSpaces = occupiedSpaces;
            this.availableSpaces = availableSpaces;
            this.costPerMinute = costPerMinute;
        }

        public String getLotId() {
            return lotId;
        }

        public String getLocation() {
            return location;
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

        public BigDecimal getCostPerMinute() {
            return costPerMinute;
        }
    }
}

