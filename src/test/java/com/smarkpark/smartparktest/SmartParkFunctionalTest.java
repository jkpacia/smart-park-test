package com.smarkpark.smartparktest;

import com.smarkpark.smartparktest.lot.ParkingLotRepository;
import com.smarkpark.smartparktest.parking.ActiveParkingRepository;
import com.smarkpark.smartparktest.parking.ExpirationScheduler;
import com.smarkpark.smartparktest.parking.ParkingSessionRepository;
import com.smarkpark.smartparktest.parking.SessionStatus;
import com.smarkpark.smartparktest.vehicle.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(SmartParkFunctionalTest.TimeConfig.class)
class SmartParkFunctionalTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired TestClock clock;
    @Autowired ActiveParkingRepository active;
    @Autowired ParkingSessionRepository sessions;
    @Autowired VehicleRepository vehicles;
    @Autowired ParkingLotRepository lots;
    @Autowired ExpirationScheduler expiration;

    private String bearerToken;

    @BeforeEach
    void resetAndLogin() throws Exception {
        // Real requests commit independently. Clean in foreign-key order between tests.
        active.deleteAll();
        sessions.deleteAll();
        vehicles.deleteAll();
        lots.deleteAll();
        clock.reset();
        String response = mvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON).content(payload("login.json")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        bearerToken = "Bearer " + json.readTree(response).get("accessToken").asText();
    }

    @Test
    void rejectsBadCredentialsAndMissingBearerToken() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON)
                        .content(payload("login-invalid.json")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mvc.perform(get("/api/lots/LOT-A/occupancy"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void rejectsExpiredBearerToken() throws Exception {
        clock.advance(Duration.ofHours(1));
        getApi("/api/lots/LOT-A/occupancy")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void registersEmptyLotAndNormalizesVehicle() throws Exception {
        postApi("/api/lots", "lot.json").andExpect(status().isCreated())
                .andExpect(jsonPath("$.lotId").value("LOT-A"))
                .andExpect(jsonPath("$.costPerMinute").value(0.5))
                .andExpect(jsonPath("$.occupiedSpaces").value(0))
                .andExpect(jsonPath("$.availableSpaces").value(1));
        postApi("/api/vehicles", "vehicle.json").andExpect(status().isCreated())
                .andExpect(jsonPath("$.licensePlate").value("CAR-001"))
                .andExpect(jsonPath("$.type").value("CAR"))
                .andExpect(jsonPath("$.ownerName").value("Jane Doe"));
    }

    @ParameterizedTest
    @CsvSource({"/api/lots, lot.json", "/api/vehicles, vehicle.json"})
    void rejectsDuplicateRegistrations(String route, String fixture) throws Exception {
        postApi(route, fixture).andExpect(status().isCreated());
        postApi(route, fixture).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"));
    }

    @ParameterizedTest
    @CsvSource({
            "/api/lots, lot-invalid.json, VALIDATION_ERROR",
            "/api/lots, lot-fractional-capacity.json, INVALID_JSON",
            "/api/lots, lot-unknown-field.json, INVALID_JSON",
            "/api/vehicles, vehicle-invalid-type.json, INVALID_JSON",
            "/api/vehicles, vehicle-missing-owner.json, VALIDATION_ERROR",
            "/api/vehicles, malformed.json, INVALID_JSON"
    })
    void rejectsInvalidRequests(String route, String fixture, String code) throws Exception {
        postApi(route, fixture).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors").isMap())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value(route));
        assertThat(lots.count()).isZero();
        assertThat(vehicles.count()).isZero();
    }

    @Test
    void checkInAndCheckoutUpdateOccupancyAndKeepHistory() throws Exception {
        registerLotAndVehicle();
        postApi("/api/lots/LOT-A/check-ins", "parking.json")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.totalCost").value(0));
        assertOccupancy(1, 0);
        getApi("/api/lots/LOT-A/vehicles").andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].licensePlate").value("CAR-001"))
                .andExpect(jsonPath("$[0].checkedInAt").value(clock.instant().toString()));

        clock.advance(Duration.ofSeconds(61));
        postApi("/api/lots/LOT-A/check-outs", "parking.json")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CHECKED_OUT"))
                .andExpect(jsonPath("$.billedMinutes").value(2))
                .andExpect(jsonPath("$.totalCost").value(1.0));
        assertOccupancy(0, 1);
        getApi("/api/lots/LOT-A/vehicles").andExpect(jsonPath("$", hasSize(0)));
        postApi("/api/lots/LOT-A/check-outs", "parking.json")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("NOT_PARKED"));
        postApi("/api/lots/LOT-A/check-ins", "parking.json").andExpect(status().isCreated());
        assertThat(sessions.count()).isEqualTo(2);
        assertThat(vehicles.count()).isEqualTo(1);
        assertOccupancy(1, 0);
    }

    @Test
    void rejectsFullLotDoubleParkingAndWrongLotCheckout() throws Exception {
        registerLotAndVehicle();
        postApi("/api/lots", "lot-other.json").andExpect(status().isCreated());
        postApi("/api/vehicles", "vehicle-other.json").andExpect(status().isCreated());
        postApi("/api/lots/LOT-A/check-ins", "parking.json").andExpect(status().isCreated());
        postApi("/api/lots/LOT-A/check-ins", "parking-other.json")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("LOT_FULL"));
        postApi("/api/lots/LOT-B/check-ins", "parking.json")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("ALREADY_PARKED"));
        postApi("/api/lots/LOT-B/check-outs", "parking.json")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("WRONG_LOT"));
        assertOccupancy(1, 0);
        assertThat(sessions.count()).isEqualTo(1);
    }

    @Test
    void rejectsUnknownLotAndUnregisteredVehicle() throws Exception {
        getApi("/api/lots/MISSING/occupancy").andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        postApi("/api/lots", "lot.json").andExpect(status().isCreated());
        postApi("/api/lots/LOT-A/check-ins", "parking.json")
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND"));
        assertOccupancy(0, 1);
    }

    @ParameterizedTest
    @CsvSource({
            "PT0S, 0, 0.0, CHECKED_OUT",
            "PT59S, 1, 0.5, CHECKED_OUT",
            "PT60S, 1, 0.5, CHECKED_OUT",
            "PT60.001S, 2, 1.0, CHECKED_OUT",
            "PT61S, 2, 1.0, CHECKED_OUT",
            "PT150S, 3, 1.5, CHECKED_OUT",
            "PT-1S, 0, 0.0, CHECKED_OUT",
            "PT15M, 15, 7.5, CHECKED_OUT",
            "PT900.001S, 0, 0.0, EXPIRED"
    })
    void billsStartedMinutesAndExpiresOnlyAfterFifteenMinutes(
            String elapsed, int minutes, double cost, String expectedStatus) throws Exception {
        registerLotAndVehicle();
        postApi("/api/lots/LOT-A/check-ins", "parking.json").andExpect(status().isCreated());
        clock.advance(Duration.parse(elapsed));
        postApi("/api/lots/LOT-A/check-outs", "parking.json").andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(expectedStatus))
                .andExpect(jsonPath("$.billedMinutes").value(minutes))
                .andExpect(jsonPath("$.totalCost").value(cost));
        assertOccupancy(0, 1);
    }

    @Test
    void expirationFreesSpaceOnceAndKeepsSessionHistory() throws Exception {
        registerLotAndVehicle();
        postApi("/api/lots/LOT-A/check-ins", "parking.json").andExpect(status().isCreated());
        clock.advance(Duration.ofMinutes(15));
        expiration.expireOverdue();
        assertOccupancy(1, 0);
        clock.advance(Duration.ofMillis(1));
        expiration.expireOverdue();
        expiration.expireOverdue();
        assertOccupancy(0, 1);
        var history = sessions.findAll();
        assertThat(history).hasSize(1);
        assertThat(history.get(0).getStatus()).isEqualTo(SessionStatus.EXPIRED);
        assertThat(history.get(0).getTotalCost()).isZero();
        postApi("/api/lots/LOT-A/check-outs", "parking.json")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("NOT_PARKED"));
        postApi("/api/lots/LOT-A/check-ins", "parking.json").andExpect(status().isCreated());
        assertOccupancy(1, 0);
        assertThat(sessions.count()).isEqualTo(2);
    }

    private void registerLotAndVehicle() throws Exception {
        postApi("/api/lots", "lot.json").andExpect(status().isCreated());
        postApi("/api/vehicles", "vehicle.json").andExpect(status().isCreated());
    }

    private void assertOccupancy(int occupied, int available) throws Exception {
        getApi("/api/lots/LOT-A/occupancy").andExpect(status().isOk())
                .andExpect(jsonPath("$.occupiedSpaces").value(occupied))
                .andExpect(jsonPath("$.availableSpaces").value(available));
    }

    private ResultActions postApi(String route, String fixture) throws Exception {
        return mvc.perform(post(route).header("Authorization", bearerToken)
                .contentType(APPLICATION_JSON).content(payload(fixture)));
    }

    private ResultActions getApi(String route) throws Exception {
        return mvc.perform(get(route).header("Authorization", bearerToken));
    }

    private byte[] payload(String name) throws IOException {
        return new ClassPathResource("payloads/" + name).getContentAsByteArray();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TimeConfig {
        @Bean
        @Primary
        TestClock testClock() {
            return new TestClock();
        }
    }

    static class TestClock extends Clock {
        private Instant now;

        void reset() { now = Instant.parse("2026-01-01T00:00:00Z"); }
        void advance(Duration duration) { now = now.plus(duration); }
        @Override public Instant instant() { return now; }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return Clock.fixed(now, zone); }
    }
}
