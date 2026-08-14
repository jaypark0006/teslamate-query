package com.teslamate.query.api.v1;

import com.teslamate.query.exception.GlobalExceptionHandler;
import com.teslamate.query.service.DriveService;
import com.teslamate.query.service.QuerySupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
