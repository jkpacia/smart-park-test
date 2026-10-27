package com.smarkpark.smartparktest.parking;

import com.smarkpark.smartparktest.lot.ParkingLot;
import com.smarkpark.smartparktest.vehicle.Vehicle;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "parking_sessions")
public class ParkingSession {
    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_plate", nullable = false)
    private Vehicle vehicle;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lot_id", nullable = false)
    private ParkingLot lot;
    @Column(nullable = false, columnDefinition = "timestamp(9)")
    private Instant checkedInAt;
    @Column(columnDefinition = "timestamp(9)")
    private Instant checkedOutAt;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal costPerMinute;
    @Column(nullable = false)
    private long billedMinutes;
    @Column(nullable = false, precision = 21, scale = 2)
    private BigDecimal totalCost = BigDecimal.ZERO.setScale(2);
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionStatus status = SessionStatus.ACTIVE;

    protected ParkingSession() {
    }

    public ParkingSession(Vehicle vehicle, ParkingLot lot, Instant checkedInAt) {
        this.vehicle = vehicle;
        this.lot = lot;
        this.checkedInAt = checkedInAt;
        this.costPerMinute = lot.getCostPerMinute();
    }

    public void close(Instant time, SessionStatus status, long minutes) {
        this.checkedOutAt = time;
        this.status = status;
        this.billedMinutes = minutes;
        this.totalCost = costPerMinute.multiply(BigDecimal.valueOf(minutes)).setScale(2);
    }

    public UUID getId() {
        return id;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public ParkingLot getLot() {
        return lot;
    }

    public Instant getCheckedInAt() {
        return checkedInAt;
    }

    public Instant getCheckedOutAt() {
        return checkedOutAt;
    }

    public BigDecimal getCostPerMinute() {
        return costPerMinute;
    }

    public long getBilledMinutes() {
        return billedMinutes;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public SessionStatus getStatus() {
        return status;
    }
}

