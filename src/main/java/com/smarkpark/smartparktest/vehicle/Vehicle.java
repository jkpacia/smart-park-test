package com.smarkpark.smartparktest.vehicle;

import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;


@Entity
@Table(name = "vehicles")
public class Vehicle implements Persistable<String> {
    @Transient
    private boolean newEntity = true;

    @Override
    public String getId() {
        return licensePlate;
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
    @Column(columnDefinition = "varchar")
    private String licensePlate;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VehicleType type;
    @Lob
    @Column(nullable = false)
    private String ownerName;

    protected Vehicle() {
    }

    public Vehicle(String licensePlate, VehicleType type, String ownerName) {
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

