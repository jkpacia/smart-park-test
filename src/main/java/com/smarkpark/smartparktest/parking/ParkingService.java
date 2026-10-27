package com.smarkpark.smartparktest.parking;

import com.smarkpark.smartparktest.common.ApiException;
import com.smarkpark.smartparktest.lot.ParkingLot;
import com.smarkpark.smartparktest.lot.ParkingLotRepository;
import com.smarkpark.smartparktest.vehicle.Vehicle;
import com.smarkpark.smartparktest.vehicle.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ParkingService {
    private static final Duration MAX_STAY = Duration.ofMinutes(15);
    private final ParkingLotRepository lots;
    private final VehicleRepository vehicles;
    private final ParkingSessionRepository sessions;
    private final ActiveParkingRepository active;
    private final Clock clock;

    public ParkingService(ParkingLotRepository lots, VehicleRepository vehicles, ParkingSessionRepository sessions, ActiveParkingRepository active, Clock clock) {
        this.lots = lots;
        this.vehicles = vehicles;
        this.sessions = sessions;
        this.active = active;
        this.clock = clock;
    }

    @Transactional
    public ParkingController.Receipt checkIn(String lotId, String plate) {
        Vehicle vehicle = vehicles.findLocked(plate).orElseThrow(() -> ApiException.missing("Vehicle"));
        ParkingLot lot = lots.findLocked(lotId).orElseThrow(() -> ApiException.missing("Parking lot"));
        if (active.existsById(plate)) throw ApiException.conflict("ALREADY_PARKED", "Vehicle is already parked");
        if (active.countByLot_LotId(lotId) >= lot.getCapacity())
            throw ApiException.conflict("LOT_FULL", "Parking lot is full");
        ParkingSession session = sessions.saveAndFlush(new ParkingSession(vehicle, lot, clock.instant()));
        active.saveAndFlush(new ActiveParking(session));
        return receipt(session);
    }

    @Transactional
    public ParkingController.Receipt checkOut(String lotId, String plate) {
        vehicles.findLocked(plate).orElseThrow(() -> ApiException.missing("Vehicle"));
        lots.findLocked(lotId).orElseThrow(() -> ApiException.missing("Parking lot"));
        ActiveParking parking = active.findById(plate).orElseThrow(() -> ApiException.conflict("NOT_PARKED", "Vehicle has no active session"));
        if (!parking.getLot().getLotId().equals(lotId))
            throw ApiException.conflict("WRONG_LOT", "Vehicle is parked in another lot");
        return close(parking, clock.instant());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean expire(String plate) {
        // Every writer locks vehicle then lot. Recheck the current session after locking.
        if (!vehicles.findLocked(plate).isPresent()) return false;
        ActiveParking parking = active.findById(plate).orElse(null);
        if (parking == null) return false;
        lots.findLocked(parking.getLot().getLotId()).orElseThrow(() -> ApiException.missing("Parking lot"));
        Instant now = clock.instant();
        if (Duration.between(parking.getSession().getCheckedInAt(), now).compareTo(MAX_STAY) <= 0) return false;
        close(parking, now);
        return true;
    }

    @Transactional(readOnly = true)
    public ParkingController.Occupancy occupancy(String lotId) {
        ParkingLot lot = lots.findById(lotId).orElseThrow(() -> ApiException.missing("Parking lot"));
        long occupied = active.countByLot_LotId(lotId);
        return new ParkingController.Occupancy(lotId, lot.getCapacity(), occupied, lot.getCapacity() - occupied);
    }

    @Transactional(readOnly = true)
    public List<ParkingController.ParkedVehicle> parkedVehicles(String lotId) {
        if (!lots.existsById(lotId)) throw ApiException.missing("Parking lot");
        return active.findParked(lotId).stream().map(a -> {
            Vehicle v = a.getSession().getVehicle();
            return new ParkingController.ParkedVehicle(v.getLicensePlate(), v.getType(), v.getOwnerName(), a.getSession().getCheckedInAt());
        }).collect(Collectors.toList());
    }

    private ParkingController.Receipt close(ActiveParking parking, Instant now) {
        ParkingSession session = parking.getSession();
        Duration elapsed = Duration.between(session.getCheckedInAt(), now);
        boolean expired = elapsed.compareTo(MAX_STAY) > 0;
        long minutes = expired || elapsed.isNegative() ? 0 : elapsed.getSeconds() / 60
                + (elapsed.getSeconds() % 60 > 0 || elapsed.getNano() > 0 ? 1 : 0);
        session.close(now, expired ? SessionStatus.EXPIRED : SessionStatus.CHECKED_OUT, minutes);
        active.delete(parking);
        return receipt(session);
    }

    private ParkingController.Receipt receipt(ParkingSession s) {
        long elapsed = s.getCheckedOutAt() == null ? 0 : Math.max(0, Duration.between(s.getCheckedInAt(), s.getCheckedOutAt()).getSeconds());
        return new ParkingController.Receipt(s.getId(), s.getVehicle().getLicensePlate(), s.getLot().getLotId(), s.getStatus(),
                s.getCheckedInAt(), s.getCheckedOutAt(), elapsed, s.getBilledMinutes(), s.getCostPerMinute(), s.getTotalCost());
    }
}

