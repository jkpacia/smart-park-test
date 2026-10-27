package com.smarkpark.smartparktest.parking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;

@Component
public class ExpirationScheduler {
    private static final Logger log=LoggerFactory.getLogger(ExpirationScheduler.class);
    private final ActiveParkingRepository active;
    private final ParkingService service;
    private final Clock clock;
    public ExpirationScheduler(ActiveParkingRepository active,ParkingService service,Clock clock){
        this.active=active;this.service=service;this.clock=clock;
    }
    @Scheduled(fixedDelayString="${smartpark.expiration.delay-ms:1000}")
    public void expireOverdue(){
        for(String plate:active.findOverduePlates(clock.instant().minus(Duration.ofMinutes(15)))){
            try { service.expire(plate); }
            catch(RuntimeException e) { log.warn("Expiration failed for {}; retrying on a later pass",plate,e); }
        }
    }
}
