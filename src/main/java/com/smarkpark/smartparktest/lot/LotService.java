package com.smarkpark.smartparktest.lot;

import com.smarkpark.smartparktest.common.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LotService {
    private final ParkingLotRepository lots;

    public LotService(ParkingLotRepository lots) {
        this.lots = lots;
    }

    @Transactional
    public LotController.LotResponse register(LotController.RegisterLot r) {
        if (lots.existsById(r.getLotId())) throw ApiException.conflict("DUPLICATE_RESOURCE", "Lot ID already exists");
        ParkingLot lot = lots.saveAndFlush(new ParkingLot(r.getLotId(), r.getLocation(), r.getCapacity(), r.getCostPerMinute()));
        return new LotController.LotResponse(lot.getLotId(), lot.getLocation(), lot.getCapacity(), 0, lot.getCapacity(), lot.getCostPerMinute());
    }
}

