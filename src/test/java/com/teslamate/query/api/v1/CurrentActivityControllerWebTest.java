package com.teslamate.query.api.v1;

import com.teslamate.query.dto.ActivityStatus;
import com.teslamate.query.dto.CurrentParkingDto;
import com.teslamate.query.dto.CurrentStatusDto;
import com.teslamate.query.exception.GlobalExceptionHandler;
import com.teslamate.query.exception.NotFoundException;
import com.teslamate.query.service.CurrentActivityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentActivityControllerWebTest {

    @Mock
    private CurrentActivityService service;

    private WebTestClient client;

    @BeforeEach
    void setUp() {
        client = WebTestClient.bindToController(new CurrentActivityController(service))
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
}
