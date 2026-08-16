package com.teslamate.query.service.trip;

import com.teslamate.query.dto.CommuteSampleDto;
import com.teslamate.query.dto.CommuteTripDto;
import com.teslamate.query.entity.DriveEntity;
import com.teslamate.query.entity.PositionCommutePoint;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Align same-clock commutes by elapsed time from each drive's own start.
 * Clock window picks the outing; elapsed minutes compare traffic.
 */
public final class CommuteCompare {

    public static final int MAX_DAYS = 21;
    public static final int DEFAULT_STEP_SEC = 60;
    static final DateTimeFormatter DAY = DateTimeFormatter.ISO_LOCAL_DATE;
    static final DateTimeFormatter LOCAL_START = DateTimeFormatter.ofPattern("MM-dd HH:mm");
    static final DateTimeFormatter LOCAL_END = DateTimeFormatter.ofPattern("HH:mm");

    private CommuteCompare() {}

    public static int clockMinutes(Instant t, ZoneId zone) {
        var z = t.atZone(zone);
        return z.getHour() * 60 + z.getMinute();
    }

    public static boolean inClockWindow(int clockMin, int afterMin, int beforeMin) {
        if (afterMin <= beforeMin) {
            return clockMin >= afterMin && clockMin <= beforeMin;
        }
        return clockMin >= afterMin || clockMin <= beforeMin;
    }

    /** One completed outing per local day in the clock window; keep the longest if several. */
    public static List<DriveEntity> pickOnePerDay(
            List<DriveEntity> drives, int afterMin, int beforeMin, ZoneId zone) {
        if (drives == null || drives.isEmpty()) {
            return List.of();
        }
        Map<LocalDate, DriveEntity> best = new LinkedHashMap<>();
        for (DriveEntity d : drives) {
            if (d == null || d.id() == null || d.startDate() == null || d.endDate() == null) {
                continue;
            }
            if (!inClockWindow(clockMinutes(d.startDate(), zone), afterMin, beforeMin)) {
                continue;
            }
            LocalDate day = d.startDate().atZone(zone).toLocalDate();
            DriveEntity prev = best.get(day);
            if (prev == null || longer(d, prev)) {
                best.put(day, d);
            }
        }
        List<DriveEntity> out = new ArrayList<>(best.values());
        if (out.size() > MAX_DAYS) {
            out = new ArrayList<>(out.subList(out.size() - MAX_DAYS, out.size()));
        }
        return out;
    }

    public static CommuteTripDto toTrip(DriveEntity d, ZoneId zone) {
        var startZ = d.startDate().atZone(zone);
        var endZ = d.endDate() == null ? null : d.endDate().atZone(zone);
        return new CommuteTripDto(
                startZ.toLocalDate().format(DAY),
                d.id(),
                d.startDate(),
                d.endDate(),
                startZ.format(LOCAL_START),
                endZ == null ? null : endZ.format(LOCAL_END),
                d.distance(),
                d.durationMin());
    }

    static boolean longer(DriveEntity a, DriveEntity b) {
        double da = a.distance() == null ? 0 : a.distance();
        double db = b.distance() == null ? 0 : b.distance();
        if (da != db) {
            return da > db;
        }
        int ta = a.durationMin() == null ? 0 : a.durationMin();
        int tb = b.durationMin() == null ? 0 : b.durationMin();
        return ta > tb;
    }

    public static List<CommuteSampleDto> resample(
            DriveEntity drive,
            List<PositionCommutePoint> raw,
            int stepSec,
            Integer onlyElapsedMin,
            ZoneId zone
    ) {
        if (drive == null || drive.startDate() == null || raw == null || raw.size() < 2) {
            return List.of();
        }
        int step = Math.max(stepSec, 15);
        Instant t0 = drive.startDate();
        Double odo0 = raw.getFirst().odometer();
        String day = t0.atZone(zone).toLocalDate().format(DAY);
        String startLocal = t0.atZone(zone).format(LOCAL_START);
        Instant lastT = raw.getLast().date();
        if (lastT == null || !lastT.isAfter(t0)) {
            lastT = t0.plusSeconds(60);
        }
        long spanSec = Math.min(Duration.between(t0, lastT).getSeconds(), 3 * 3600L);
        if (onlyElapsedMin != null) {
            long want = onlyElapsedMin * 60L;
            if (want > spanSec) {
                return List.of();
            }
            PositionCommutePoint p = atElapsed(raw, t0, want);
            return p == null ? List.of() : List.of(toDto(drive.id(), day, startLocal, t0, want / 60.0, p, odo0, zone));
        }
        List<CommuteSampleDto> out = new ArrayList<>();
        for (long sec = 0; sec <= spanSec; sec += step) {
            PositionCommutePoint p = atElapsed(raw, t0, sec);
            if (p != null) {
                out.add(toDto(drive.id(), day, startLocal, t0, sec / 60.0, p, odo0, zone));
            }
        }
        return out;
    }

    /** Last sample at or before {@code elapsedSec} from {@code t0}. */
    static PositionCommutePoint atElapsed(List<PositionCommutePoint> raw, Instant t0, long elapsedSec) {
        PositionCommutePoint keep = null;
        Instant limit = t0.plusSeconds(elapsedSec);
        for (PositionCommutePoint p : raw) {
            if (p.date() == null) {
                continue;
            }
            if (p.date().isAfter(limit)) {
                break;
            }
            keep = p;
        }
        return keep != null ? keep : raw.getFirst();
    }

    static CommuteSampleDto toDto(
            Long driveId, String day, String startLocal, Instant start, double elapsedMin,
            PositionCommutePoint p, Double odo0, ZoneId zone) {
        Double km = null;
        if (p.odometer() != null && odo0 != null) {
            km = Math.max(0, p.odometer() - odo0);
        }
        Integer speed = p.speed();
        String label = speed == null
                ? startLocal
                : startLocal + " · " + speed + " km/h";
        Instant origin = java.time.LocalDate.of(2020, 1, 1).atStartOfDay(zone).toInstant();
        Instant elapsedMs = origin.plusSeconds(Math.round(elapsedMin * 60.0));
        Double lat = p.latitude() == null ? null : p.latitude().doubleValue();
        Double lon = p.longitude() == null ? null : p.longitude().doubleValue();
        return new CommuteSampleDto(day, driveId, elapsedMin, elapsedMs, start, startLocal,
                lat, lon, speed, km, label);
    }

    public static Comparator<CommuteSampleDto> byDayThenElapsed() {
        return Comparator.comparing(CommuteSampleDto::day).thenComparingDouble(CommuteSampleDto::elapsedMin);
    }
}
