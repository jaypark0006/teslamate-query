package com.teslamate.query.api.v1;

import com.teslamate.query.dto.ActivityStatus;
import com.teslamate.query.dto.CurrentParkingDto;
import com.teslamate.query.dto.CurrentStatusDto;
import com.teslamate.query.dto.RecentDriveDto;
import com.teslamate.query.exception.GlobalExceptionHandler;
import com.teslamate.query.exception.NotFoundException;
import com.teslamate.query.service.CurrentActivityService;
import com.teslamate.query.service.RecentActivityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentActivityControllerWebTest {

    @Mock
    private CurrentActivityService service;
    @Mock
    private RecentActivityService recent;

    private WebTestClient client;

    @BeforeEach
    void setUp() {
        client = WebTestClient.bindToController(new CurrentActivityController(service, recent))
                .controllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void currentStatusOk() {
        when(service.status(1L)).thenReturn(new CurrentStatusDto(
                1L, "Y", "Apollo19MetallicShadow", ActivityStatus.PARKING, 100L,
                39, new BigDecimal("161.58"), 54139.1,
                new BigDecimal("106.460280"), new BigDecimal("29.518242"),
                new BigDecimal("31.0"), new BigDecimal("33.5"),
                false, new BigDecimal("21.0"),
                null, null, "2026.8.3.6"));
        client.get().uri("/api/v1/cars/1/current")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("PARKING")
                .jsonPath("$.batteryLevel").isEqualTo(39)
                .jsonPath("$.firmwareVersion").isEqualTo("2026.8.3.6");
    }

    @Test
    void currentCharging404WhenIdle() {
        when(service.charging(1L)).thenThrow(new NotFoundException("No in-progress charging for car: 1"));
        client.get().uri("/api/v1/cars/1/current/charging")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("NOT_FOUND");
    }

    @Test
    void currentParkingOk() {
        when(service.parking(1L)).thenReturn(new CurrentParkingDto(1L, 3174L, new BigDecimal("33.5")));
        client.get().uri("/api/v1/cars/1/current/parking")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.durationMin").isEqualTo(3174)
                .jsonPath("$.outsideTempC").isEqualTo(33.5);
    }

    @Test
    void recentDrivesOk() {
        when(recent.recentDrives(1L, 5, "0")).thenReturn(List.of(new RecentDriveDto(
                10L, List.of(10L),
                Instant.parse("2026-08-13T07:10:58Z"), Instant.parse("2026-08-13T07:35:44Z"),
                25, 17.0, 2.86, 20.7, 184.6, 163.9, 45, 39, 40.8, 168.0)));
        client.get().uri("/api/v1/cars/1/recent/drives?limit=5&mergeGapMin=0")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].id").isEqualTo(10)
                .jsonPath("$[0].energyUsedKwh").isEqualTo(2.86);
    }
}
