package com.teslamate.query.service.trip;

import com.teslamate.query.dto.CommuteSampleDto;
import com.teslamate.query.entity.DriveEntity;
import com.teslamate.query.entity.PositionCommutePoint;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommuteCompareTest {

    static final ZoneId SH = ZoneId.of("Asia/Shanghai");

    @Test
    void clockWindowKeepsMorningOuting() {
        // 05:48 CST = 21:48 previous day UTC
        DriveEntity morning = drive(5186, Instant.parse("2026-08-12T21:48:54Z"), 25, 15.2);
        DriveEntity hop = drive(5185, Instant.parse("2026-08-12T21:40:12Z"), 3, 0.9);
        DriveEntity afternoon = drive(5187, Instant.parse("2026-08-12T23:10:58Z"), 25, 17.0);
        List<DriveEntity> picked = CommuteCompare.pickOnePerDay(
                List.of(hop, morning, afternoon), 5 * 60 + 40, 6 * 60 + 20, SH);
        assertEquals(1, picked.size());
        assertEquals(5186L, picked.getFirst().id());
    }

    @Test
    void oneDrivePerDayPicksLonger() {
        DriveEntity shortOne = drive(1, Instant.parse("2026-08-12T21:50:00Z"), 10, 4.0);
        DriveEntity longOne = drive(2, Instant.parse("2026-08-12T21:55:00Z"), 20, 12.0);
        List<DriveEntity> picked = CommuteCompare.pickOnePerDay(
                List.of(shortOne, longOne), 5 * 60, 7 * 60, SH);
        assertEquals(2L, picked.getFirst().id());
    }

    @Test
    void resampleAtTenMinutesUsesLastPointBefore() {
        Instant t0 = Instant.parse("2026-08-12T21:48:00Z");
        DriveEntity d = drive(9, t0, 25, 15.0);
        List<PositionCommutePoint> pts = new ArrayList<>();
        pts.add(pt(9, t0, 106.45, 29.51, 0, 1000.0));
        pts.add(pt(9, t0.plusSeconds(9 * 60), 106.46, 29.55, 40, 1004.0));
        pts.add(pt(9, t0.plusSeconds(11 * 60), 106.47, 29.58, 12, 1005.0));
        pts.add(pt(9, t0.plusSeconds(20 * 60), 106.48, 29.59, 30, 1012.0));
        List<CommuteSampleDto> at10 = CommuteCompare.resample(d, pts, 60, 10, SH);
        assertEquals(1, at10.size());
        assertEquals(29.55, at10.getFirst().latitude(), 1e-6);
        assertEquals(40, at10.getFirst().speed());
        assertEquals(4.0, at10.getFirst().kmFromStart(), 1e-6);
        assertEquals("08-13 05:48", at10.getFirst().startLocal());
        assertTrue(at10.getFirst().label().startsWith("08-13 05:48"));
        assertEquals("00:10", at10.getFirst().elapsedMs().atZone(SH).toLocalTime().toString().substring(0, 5));
        assertTrue(at10.getFirst().label().contains("40"));
        var trip = CommuteCompare.toTrip(d, SH);
        assertEquals("08-13 05:48", trip.startLocal());
        assertEquals(9L, trip.driveId());
    }

    @Test
    void skipIfDriveShorterThanElapsed() {
        Instant t0 = Instant.parse("2026-08-12T21:48:00Z");
        DriveEntity d = drive(9, t0, 5, 3.0);
        List<PositionCommutePoint> pts = List.of(
                pt(9, t0, 106.45, 29.51, 10, 1.0),
                pt(9, t0.plusSeconds(4 * 60), 106.46, 29.52, 10, 2.0));
        assertTrue(CommuteCompare.resample(d, pts, 60, 10, SH).isEmpty());
    }

    private static DriveEntity drive(long id, Instant start, int durationMin, double km) {
        Instant end = start.plusSeconds(durationMin * 60L);
        return new DriveEntity(
                id, 1L, start, end,
                null, null, null, null, null,
                null, null, null, null,
                null, null,
                km, durationMin, null, null,
                1L, 2L,
                null, null, null, null);
    }

    private static PositionCommutePoint pt(
            long driveId, Instant date, double lon, double lat, int speed, double odo) {
        return new PositionCommutePoint(driveId, date, BigDecimal.valueOf(lon), BigDecimal.valueOf(lat),
                speed, odo);
    }
}
