package com.teslamate.query.api.v1;

import com.teslamate.query.dto.DayGridCellDto;
import com.teslamate.query.dto.MapPointDto;
import com.teslamate.query.dto.TimelineItemDto;
import com.teslamate.query.dto.TimelineKind;
import com.teslamate.query.exception.GlobalExceptionHandler;
import com.teslamate.query.service.QuerySupport;
import com.teslamate.query.service.TripViewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TripViewControllerWebTest {

    @Mock
    private TripViewService tripViewService;
    @Mock
    private QuerySupport support;

    private WebTestClient client;

    @BeforeEach
    void setUp() {
        client = WebTestClient.bindToController(new TripViewController(tripViewService, support))
                .controllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void timelineOk() {
        when(support.minParkMin("10")).thenReturn(10);
        when(support.zone(null)).thenReturn(java.time.ZoneId.of("Asia/Shanghai"));
        when(support.units(null, null)).thenReturn(null);
        when(tripViewService.timeline(eq(1L), eq("2026-08-16T00:00:00Z"), eq("2026-08-16T12:00:00Z"),
                eq(10), any(), any())).thenReturn(List.of(
                new TimelineItemDto(1, TimelineKind.DRIVE, 9L,
                        Instant.parse("2026-08-16T02:00:00Z"), Instant.parse("2026-08-16T03:00:00Z"),
                        60.0, "Home → Work", "12.0 km · 1h", "#3b82f6",
                        29.5, 106.4, 12.0, 70, 61, null, null, "2026-08-16", 0,
                        Instant.parse("2000-01-01T02:00:00Z"), Instant.parse("2000-01-01T03:00:00Z"))));
        client.get().uri(uriBuilder -> uriBuilder.path("/api/v1/cars/1/timeline")
                        .queryParam("from", "2026-08-16T00:00:00Z")
                        .queryParam("to", "2026-08-16T12:00:00Z")
                        .queryParam("minParkMin", "10")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].kind").isEqualTo("DRIVE")
                .jsonPath("$[0].title").isEqualTo("Home → Work")
                .jsonPath("$[0].seq").isEqualTo(1);
    }

    @Test
    void pointsOk() {
        when(support.minParkMin(null)).thenReturn(10);
        when(support.units(null, null)).thenReturn(null);
        when(tripViewService.points(eq(1L), eq("2026-08-16T00:00:00Z"), eq("2026-08-16T12:00:00Z"),
                eq(10), eq("drive"), any())).thenReturn(List.of(
                new MapPointDto(Instant.parse("2026-08-16T02:00:00Z"), 29.5, 106.4,
                        "drive", 9L, 1, 90.0, 120.0, "#3b82f6", "Home → Work", null, null, null)));
        client.get().uri(uriBuilder -> uriBuilder.path("/api/v1/cars/1/map/points")
                        .queryParam("from", "2026-08-16T00:00:00Z")
                        .queryParam("to", "2026-08-16T12:00:00Z")
                        .queryParam("kinds", "drive")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].kind").isEqualTo("drive")
                .jsonPath("$[0].latitude").isEqualTo(29.5)
                .jsonPath("$[0].heading").isEqualTo(90.0);
    }

    @Test
    void gridOk() {
        when(support.minParkMin(null)).thenReturn(10);
        when(support.zone(null)).thenReturn(java.time.ZoneId.of("Asia/Shanghai"));
        when(support.dayStartHour(null)).thenReturn(0);
        when(support.units(null, null)).thenReturn(null);
        when(tripViewService.grid(eq(1L), eq("2026-08-16T00:00:00Z"), eq("2026-08-16T12:00:00Z"),
                eq(10), any(), any(), eq(0))).thenReturn(List.of(
                new DayGridCellDto(Instant.parse("2026-08-15T16:00:00Z"), "2026-08-16",
                        8.0, "08:00", DayGridCellDto.DRIVE, "DRIVE")));
        client.get().uri(uriBuilder -> uriBuilder.path("/api/v1/cars/1/timeline/grid")
                        .queryParam("from", "2026-08-16T00:00:00Z")
                        .queryParam("to", "2026-08-16T12:00:00Z")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].kind").isEqualTo("DRIVE")
                .jsonPath("$[0].slot").isEqualTo("08:00")
                .jsonPath("$[0].kindCode").isEqualTo(2);
    }
}
