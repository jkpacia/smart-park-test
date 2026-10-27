package com.smarkpark.smartparktest.lot;

import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;

@Entity
@Table(name = "parking_lots")
public class ParkingLot implements Persistable<String> {
    @Transient
    private boolean newEntity = true;

    @Override
    public String getId() {
        return lotId;
    }

    @Override
    public boolean isNew() {
        return newEntity;
    }

    @PostLoad
    @PostPersist
    private void markPersisted() {
        newEntity = false;
    }

    @Id
    @Column(length = 50)
    private String lotId;
    @Lob
    @Column(nullable = false)
    private String location;
    @Column(nullable = false)
    private int capacity;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal costPerMinute;

    protected ParkingLot() {
    }

    public ParkingLot(String lotId, String location, int capacity, BigDecimal costPerMinute) {
        this.lotId = lotId;
        this.location = location;
        this.capacity = capacity;
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

    public BigDecimal getCostPerMinute() {
        return costPerMinute;
    }
}

