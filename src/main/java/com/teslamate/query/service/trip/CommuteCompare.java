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
 * Clock window picks the outing; kilometres from that day's start align
 * the same stretch of road across days.
 */
public final class CommuteCompare {

    public static final int MAX_DAYS = 21;
    public static final double DEFAULT_STEP_KM = 0.5;
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

    public static List<CommuteSampleDto> resampleByKm(
            DriveEntity drive, List<PositionCommutePoint> raw, double stepKm, ZoneId zone) {
        if (drive == null || drive.startDate() == null || raw == null || raw.size() < 2) {
            return List.of();
        }
        double step = Math.min(Math.max(stepKm, 0.2), 2.0);
        Instant t0 = drive.startDate();
        String day = t0.atZone(zone).toLocalDate().format(DAY);
        String startLocal = t0.atZone(zone).format(LOCAL_START);
        double[] along = kmAlong(raw);
        double total = along[along.length - 1];
        if (total < step / 2) {
            return List.of();
        }
        List<CommuteSampleDto> out = new ArrayList<>();
        for (double km = 0; km <= total + 1e-6; km += step) {
            int idx = firstAtOrAfter(along, km);
            out.add(toDto(drive, day, startLocal, raw.get(idx), km, t0, zone));
        }
        return out;
    }

    static double[] kmAlong(List<PositionCommutePoint> raw) {
        double[] km = new double[raw.size()];
        for (int i = 1; i < raw.size(); i++) {
            km[i] = km[i - 1] + havKm(raw.get(i - 1), raw.get(i));
        }
        return km;
    }

    /** Point where the car has reached this many km (same stretch across days). */
    static int firstAtOrAfter(double[] along, double km) {
        for (int i = 0; i < along.length; i++) {
            if (along[i] + 1e-9 >= km) {
                return i;
            }
        }
        return along.length - 1;
    }

    static double havKm(PositionCommutePoint a, PositionCommutePoint b) {
        if (a.latitude() == null || a.longitude() == null
                || b.latitude() == null || b.longitude() == null) {
            return 0;
        }
        return havKm(
                a.latitude().doubleValue(), a.longitude().doubleValue(),
                b.latitude().doubleValue(), b.longitude().doubleValue());
    }

    static double havKm(double lat1, double lon1, double lat2, double lon2) {
        double r = 6371.0;
        double p1 = Math.toRadians(lat1);
        double p2 = Math.toRadians(lat2);
        double dlat = p2 - p1;
        double dlon = Math.toRadians(lon2 - lon1);
        double h = Math.sin(dlat / 2) * Math.sin(dlat / 2)
                + Math.cos(p1) * Math.cos(p2) * Math.sin(dlon / 2) * Math.sin(dlon / 2);
        return 2 * r * Math.asin(Math.min(1, Math.sqrt(h)));
    }

    static CommuteSampleDto toDto(
            DriveEntity drive, String day, String startLocal, PositionCommutePoint p,
            double km, Instant t0, ZoneId zone) {
        double elapsedMin = 0;
        if (p.date() != null && t0 != null && p.date().isAfter(t0)) {
            elapsedMin = Duration.between(t0, p.date()).toMillis() / 60_000.0;
        }
        Integer speed = p.speed();
        String bin = String.format(java.util.Locale.US, "%.1f", km);
        String label = startLocal + " · " + bin + " km"
                + (speed == null ? "" : " · " + speed + " km/h");
        Double lat = p.latitude() == null ? null : p.latitude().doubleValue();
        Double lon = p.longitude() == null ? null : p.longitude().doubleValue();
        return new CommuteSampleDto(
                day, drive.id(), elapsedMin, t0, startLocal,
                lat, lon, speed, km, bin, label);
    }

    public static Comparator<CommuteSampleDto> byDayThenKm() {
        return Comparator.comparing(CommuteSampleDto::day)
                .thenComparingDouble(s -> s.kmFromStart() == null ? 0 : s.kmFromStart());
    }
}
