package com.smarkpark.smartparktest.parking;


import com.smarkpark.smartparktest.lot.ParkingLot;
import jakarta.persistence.*;

@Entity
@Table(name = "active_parking", indexes = @Index(name = "active_lot_idx", columnList = "lot_id"))
public class ActiveParking {
    @Id
    @Column(columnDefinition = "varchar")
    private String licensePlate;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false, unique = true)
    private ParkingSession session;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lot_id", nullable = false)
    private ParkingLot lot;

    protected ActiveParking() {
    }

    public ActiveParking(ParkingSession session) {
        this.session = session;
        this.licensePlate = session.getVehicle().getLicensePlate();
        this.lot = session.getLot();
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public ParkingSession getSession() {
        return session;
    }

    public ParkingLot getLot() {
        return lot;
    }
}

