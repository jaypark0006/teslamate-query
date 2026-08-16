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
    void oneDrivePerDayPicksLonger() {
        DriveEntity shortOne = drive(1, Instant.parse("2026-08-12T21:50:00Z"), 10, 4.0);
        DriveEntity longOne = drive(2, Instant.parse("2026-08-12T21:55:00Z"), 20, 12.0);
        List<DriveEntity> picked = CommuteCompare.pickOnePerDay(
                List.of(shortOne, longOne), 5 * 60, 7 * 60, SH);
        assertEquals(2L, picked.getFirst().id());
    }

    @Test
    void resampleByKmAlignsSameStretch() {
        Instant t0 = Instant.parse("2026-08-12T21:48:00Z");
        DriveEntity d = drive(9, t0, 25, 15.0);
        // ~1.11 km due north
        List<PositionCommutePoint> pts = List.of(
                pt(9, t0, 106.45, 29.50, 50),
                pt(9, t0.plusSeconds(180), 106.45, 29.51, 12));
        List<CommuteSampleDto> samples = CommuteCompare.resampleByKm(d, pts, 0.5, SH);
        assertTrue(samples.size() >= 3);
        assertEquals("0.0km", samples.getFirst().kmBin());
        assertEquals(50, samples.getFirst().speed());
        assertEquals("08-13 05:48", samples.getFirst().startLocal());
        CommuteSampleDto last = samples.getLast();
        assertEquals(12, last.speed());
        assertTrue(last.kmFromStart() >= 1.0);
        var trip = CommuteCompare.toTrip(d, SH);
        assertEquals("08-13 05:48", trip.startLocal());
        assertEquals(9L, trip.driveId());
    }

    @Test
    void twoDaysShareTheSameKmColumns() {
        Instant a0 = Instant.parse("2026-08-12T21:48:00Z");
        Instant b0 = Instant.parse("2026-08-13T21:50:00Z");
        var a = CommuteCompare.resampleByKm(drive(1, a0, 20, 12), List.of(
                pt(1, a0, 106.45, 29.50, 40),
                pt(1, a0.plusSeconds(200), 106.45, 29.51, 15)), 0.5, SH);
        var b = CommuteCompare.resampleByKm(drive(2, b0, 22, 12), List.of(
                pt(2, b0, 106.45, 29.50, 38),
                pt(2, b0.plusSeconds(400), 106.45, 29.51, 8)), 0.5, SH);
        assertEquals(a.stream().map(CommuteSampleDto::kmBin).toList(),
                b.stream().map(CommuteSampleDto::kmBin).toList());
        assertEquals(15, a.getLast().speed());
        assertEquals(8, b.getLast().speed());
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
