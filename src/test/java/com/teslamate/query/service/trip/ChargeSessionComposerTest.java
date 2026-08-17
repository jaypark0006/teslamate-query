package com.teslamate.query.service.trip;

import com.teslamate.query.entity.ChargingProcessEntity;
import com.teslamate.query.entity.DriveEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChargeSessionComposerTest {

    @Test
    void mergesNearbyFragmentsAtSameAddress() {
        var sessions = ChargeSessionComposer.newestSessions(
                List.of(process(1, "10:00", "10:20", 7L), process(2, "10:25", "10:50", 7L)),
                Map.of(), List.of(), 15, 100, 5);

        assertEquals(1, sessions.size());
        assertEquals(List.of(1L, 2L), sessions.getFirst().stream().map(ChargingProcessEntity::id).toList());
    }

    @Test
    void keepsDifferentLocationsSeparate() {
        var sessions = ChargeSessionComposer.newestSessions(
                List.of(process(1, "10:00", "10:20", 7L), process(2, "10:25", "10:50", 8L)),
                Map.of(), List.of(), 15, 100, 5);

        assertEquals(2, sessions.size());
    }

    @Test
    void driveBetweenFragmentsPreventsMerge() {
        var drive = new DriveEntity(
                9L, 1L, time("10:21"), time("10:24"), null, null, null, null, null,
                null, null, null, null, null, null, null, 3, null, null,
                null, null, null, null, null, null);
        var sessions = ChargeSessionComposer.newestSessions(
                List.of(process(1, "10:00", "10:20", 7L), process(2, "10:25", "10:50", 7L)),
                Map.of(), List.of(drive), 15, 100, 5);

        assertEquals(2, sessions.size());
    }

    private static ChargingProcessEntity process(long id, String start, String end, Long addressId) {
        return new ChargingProcessEntity(
                id, 1L, time(start), time(end), null, null,
                null, null, null, null, null, null, null, null, null,
                id, addressId, null);
    }

    private static LocalDateTime time(String time) {
        return LocalDateTime.parse("2026-08-17T" + time);
    }
}
