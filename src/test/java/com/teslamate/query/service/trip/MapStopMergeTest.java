package com.teslamate.query.service.trip;

import com.teslamate.query.dto.MapPointDto;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapStopMergeTest {

    @Test
    void sameSpotParksBecomeOneMarker() {
        List<MapPointDto> out = MapStopMerge.mergeStops(List.of(
                park(29.5182, 106.4603, 60),
                park(29.5184, 106.4605, 120)));
        assertEquals(1, out.stream().filter(p -> "park".equals(p.kind())).count());
        assertEquals("P ×2 · 3h", out.getFirst().durationLabel());
    }

    @Test
    void farApartParksStaySeparate() {
        List<MapPointDto> out = MapStopMerge.mergeStops(List.of(
                park(29.52, 106.46, 30),
                park(29.60, 106.55, 45)));
        assertEquals(2, out.size());
    }

    @Test
    void chargeLabelMatchesMockup() {
        assertEquals("AC +32.4 kWh · 30 min", MapStopMerge.chargeLabel(1, "AC", 32.4, 30.0));
        assertEquals("P 14h", MapStopMerge.parkLabel(1, 14 * 60.0, false));
        assertEquals("P -", MapStopMerge.parkLabel(1, null, true));
    }

    @Test
    void drivesAreNotMerged() {
        MapPointDto d1 = drive(29.5, 106.4);
        MapPointDto d2 = drive(29.5001, 106.4001);
        List<MapPointDto> out = MapStopMerge.mergeStops(List.of(d1, d2));
        assertEquals(2, out.size());
        assertTrue(out.stream().allMatch(p -> "drive".equals(p.kind())));
    }

    private static MapPointDto park(double lat, double lon, double min) {
        return new MapPointDto(Instant.parse("2026-08-01T00:00:00Z"), lat, lon, "park",
                1L, 1, null, null, "#1d4ed8", "Parked", "P " + (int) min + " min", min, null);
    }

    private static MapPointDto drive(double lat, double lon) {
        return new MapPointDto(Instant.parse("2026-08-01T00:00:00Z"), lat, lon, "drive",
                1L, 1, 90.0, 1.0, "#3b82f6", "Drive", null, null, null);
    }
}
