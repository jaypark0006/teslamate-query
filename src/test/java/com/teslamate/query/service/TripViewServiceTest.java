package com.teslamate.query.service;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TripViewServiceTest {

    @Test
    void parseKindsDefaultAndFilter() {
        assertEquals(Set.of("drive", "charge", "park"), TripViewService.parseKinds(null));
        assertEquals(Set.of("drive", "charge", "park"), TripViewService.parseKinds("${kinds}"));
        assertEquals(Set.of("drive"), TripViewService.parseKinds("drive"));
        assertEquals(Set.of("charge", "park"), TripViewService.parseKinds("charge,park"));
    }

    @Test
    void parkDurationIsDashWhileStillParked() {
        Instant now = Instant.parse("2026-08-16T12:00:00Z");
        assertTrue(TripViewService.liveWindow(now, now));
        assertTrue(TripViewService.openEnded(now, now));
        assertEquals("-", TripViewService.parkDurationLabel(180, true));
        assertEquals("3h", TripViewService.parkDurationLabel(180, false));
        assertEquals("38 min", TripViewService.parkDurationLabel(38, false));
        assertFalse(TripViewService.liveWindow(Instant.parse("2026-08-10T00:00:00Z"), now));
    }
}
