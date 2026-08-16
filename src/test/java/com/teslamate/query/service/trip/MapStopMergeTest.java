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
        assertEquals("P×2 3h", out.getFirst().durationLabel());
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
        assertEquals("AC 32.4kWh 30m", MapStopMerge.chargeLabel(1, "AC", 32.4, 30.0));
        assertEquals("P 14h", MapStopMerge.parkLabel(1, 14 * 60.0, false));
        assertEquals("P", MapStopMerge.parkLabel(1, null, true));
        assertEquals(null, MapStopMerge.parkLabel(1, 14.0, false));
    }

    @Test
    void sameSpotAcAndDcGetTwoLabelLines() {
        List<MapPointDto> out = MapStopMerge.mergeStops(List.of(
                charge(29.5182, 106.4603, "DC", 10.0, 20),
                charge(29.5183, 106.4604, "DC", 12.0, 25),
                charge(29.5182, 106.4603, "AC", 8.0, 120)));
        assertEquals(1, out.size());
        assertEquals("DC×2 22kWh 45m\nAC 8kWh 2h", out.getFirst().durationLabel());
        assertEquals(22.0 + 8.0, out.getFirst().energyKwh());
        assertEquals(20.0 + 25.0 + 120.0, out.getFirst().durationMin());
    }

    @Test
    void sameSpotDcOnlyStaysOneLine() {
        List<MapPointDto> out = MapStopMerge.mergeStops(List.of(
                charge(29.5182, 106.4603, "DC", 10.0, 20),
                charge(29.5182, 106.4603, "DC", 5.0, 10)));
        assertEquals(1, out.size());
        assertEquals("DC×2 15kWh 30m", out.getFirst().durationLabel());
    }

    @Test
    void drivesAreNotMerged() {
        MapPointDto d1 = drive(29.5, 106.4);
        MapPointDto d2 = drive(29.5001, 106.4001);
        List<MapPointDto> out = MapStopMerge.mergeStops(List.of(d1, d2));
        assertEquals(2, out.size());
        assertTrue(out.stream().allMatch(p -> "drive".equals(p.kind())));
    }

    private static MapPointDto charge(double lat, double lon, String type, double kwh, double min) {
        return new MapPointDto(Instant.parse("2026-08-01T00:00:00Z"), lat, lon, "charge",
                1L, 1, null, null, "#22c55e", type + " charge",
                MapStopMerge.chargeLabel(1, type, kwh, min), min, kwh);
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
