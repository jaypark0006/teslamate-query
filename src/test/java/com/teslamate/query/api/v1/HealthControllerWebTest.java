package com.teslamate.query.api.v1;

import com.teslamate.query.dao.HealthDao;
import com.teslamate.query.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthControllerWebTest {

    @Mock
    private HealthDao healthDao;

    private WebTestClient client;

    @BeforeEach
    void setUp() {
        client = WebTestClient.bindToController(new HealthController(healthDao))
                .controllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void healthIsPublicAndUp() {
        when(healthDao.ping()).thenReturn(1);
        client.get().uri("/api/v1/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP")
                .jsonPath("$.database").isEqualTo("UP");
    }

    @Test
    void healthDownWhenPingFails() {
        when(healthDao.ping()).thenThrow(new RuntimeException("db"));
        client.get().uri("/api/v1/health")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.status").isEqualTo("DOWN")
                .jsonPath("$.database").isEqualTo("DOWN");
    }
}
