package com.teslamate.query.service.trip;

import com.teslamate.query.dto.CommuteSampleDto;
import com.teslamate.query.entity.DriveEntity;
import com.teslamate.query.entity.PositionCommutePoint;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommuteCompareTest {

    static final ZoneId SH = ZoneId.of("Asia/Shanghai");

    @Test
    void clockWindowKeepsMorningOuting() {
        DriveEntity morning = drive(5186, Instant.parse("2026-08-12T21:48:54Z"), 25, 15.2);
        DriveEntity hop = drive(5185, Instant.parse("2026-08-12T21:40:12Z"), 3, 0.9);
        DriveEntity afternoon = drive(5187, Instant.parse("2026-08-12T23:10:58Z"), 25, 17.0);
        List<DriveEntity> picked = CommuteCompare.pickOnePerDay(
                List.of(hop, morning, afternoon), 5 * 60 + 40, 6 * 60 + 20, SH);
        assertEquals(1, picked.size());
        assertEquals(5186L, picked.getFirst().id());
    }

    @Test
    void atTenMinutesUsesLastPointBefore() {
        Instant t0 = Instant.parse("2026-08-12T21:48:00Z");
        DriveEntity d = drive(9, t0, 25, 15.0);
        List<PositionCommutePoint> pts = List.of(
                pt(9, t0, 106.45, 29.50, 40),
                pt(9, t0.plusSeconds(9 * 60), 106.46, 29.55, 20),
                pt(9, t0.plusSeconds(11 * 60), 106.47, 29.58, 55));
        List<CommuteSampleDto> samples = CommuteCompare.resampleByElapsed(d, pts, 60, SH);
        CommuteSampleDto at10 = samples.stream()
                .filter(s -> "10min".equals(s.elapsedBin()))
                .findFirst()
                .orElseThrow();
        assertEquals(20, at10.speed());
        assertEquals("08-13 05:48", at10.startLocal());
        assertTrue(at10.kmFromStart() > 0);
        assertEquals("08-13 05:48", CommuteCompare.toTrip(d, SH).startLocal());
    }

    @Test
    void twoDaysShareTheSameMinuteColumns() {
        Instant a0 = Instant.parse("2026-08-12T21:48:00Z");
        Instant b0 = Instant.parse("2026-08-13T21:50:00Z");
        var a = CommuteCompare.resampleByElapsed(drive(1, a0, 12, 8), List.of(
                pt(1, a0, 106.45, 29.50, 40),
                pt(1, a0.plusSeconds(10 * 60), 106.46, 29.56, 30)), 60, SH);
        var b = CommuteCompare.resampleByElapsed(drive(2, b0, 12, 8), List.of(
                pt(2, b0, 106.45, 29.50, 35),
                pt(2, b0.plusSeconds(10 * 60), 106.455, 29.53, 10)), 60, SH);
        assertEquals(a.stream().map(CommuteSampleDto::elapsedBin).toList(),
                b.stream().map(CommuteSampleDto::elapsedBin).toList());
        double kmA = a.stream().filter(s -> "10min".equals(s.elapsedBin())).findFirst().orElseThrow().kmFromStart();
        double kmB = b.stream().filter(s -> "10min".equals(s.elapsedBin())).findFirst().orElseThrow().kmFromStart();
        assertTrue(kmA > kmB, "faster day should have gone farther by minute 10");
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
            long driveId, Instant date, double lon, double lat, int speed) {
        return new PositionCommutePoint(driveId, date, BigDecimal.valueOf(lon), BigDecimal.valueOf(lat),
                speed, null);
    }
}
