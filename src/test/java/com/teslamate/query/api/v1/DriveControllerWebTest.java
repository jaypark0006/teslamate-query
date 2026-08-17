package com.teslamate.query.api.v1;

import com.teslamate.query.dto.TirePressureDto;
import com.teslamate.query.dto.TirePressureSampleDto;
import com.teslamate.query.exception.GlobalExceptionHandler;
import com.teslamate.query.service.DriveService;
import com.teslamate.query.service.QuerySupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriveControllerWebTest {

    @Mock
    private DriveService driveService;
    @Mock
    private QuerySupport support;

    private WebTestClient client;

    @BeforeEach
    void setUp() {
        client = WebTestClient.bindToController(new DriveController(driveService, support))
                .controllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void unsubstitutedGrafanaCarIdIsBadRequest() {
        client.get()
                .uri(URI.create("/api/v1/drives?carId=%24%7Bcar_id%7D"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("BAD_REQUEST")
                .jsonPath("$.message").value(msg -> assertTrue(
                        String.valueOf(msg).contains("${car_id}"),
                        () -> "expected message to mention ${car_id}, got: " + msg));
    }

    @Test
    void tirePressureHistoryUsesDedicatedEndpoint() {
        when(driveService.tirePressures(5096L, 5)).thenReturn(List.of(
                new TirePressureSampleDto(
                        Instant.parse("2026-08-17T12:00:00Z"),
                        new TirePressureDto(
                                new BigDecimal("2.9"), new BigDecimal("3.0"),
                                new BigDecimal("3.1"), new BigDecimal("3.2")))));

        client.get()
                .uri("/api/v1/drives/5096/tire-pressures?downsample=5")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].date").isEqualTo("2026-08-17T12:00:00Z")
                .jsonPath("$[0].tirePressure.fl").isEqualTo(2.9)
                .jsonPath("$[0].tirePressure.fr").isEqualTo(3.0)
                .jsonPath("$[0].tirePressure.rl").isEqualTo(3.1)
                .jsonPath("$[0].tirePressure.rr").isEqualTo(3.2);
    }
}
