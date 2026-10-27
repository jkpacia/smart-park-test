package com.smarkpark.smartparktest.parking;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "smartpark.expiration.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfiguration {
}
