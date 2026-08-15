package com.teslamate.query.service.trip;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/** Split activity spans at local midnight so a 3-day park becomes one piece per day. */
public final class DaySplit {

    private DaySplit() {}

    public static List<ActivitySpan> splitByLocalDays(List<ActivitySpan> spans, ZoneId zone) {
        if (spans == null || spans.isEmpty()) {
            return List.of();
        }
        ZoneId z = zone == null ? ZoneId.of("UTC") : zone;
        List<ActivitySpan> out = new ArrayList<>();
        for (ActivitySpan span : spans) {
            out.addAll(splitOne(span, z));
        }
        return List.copyOf(out);
    }

    public static String dayLabel(Instant instant, ZoneId zone) {
        return localDate(instant, zone).toString();
    }

    public static int dayBand(Instant instant, ZoneId zone) {
        return (int) Math.floorMod(localDate(instant, zone).toEpochDay(), 2);
    }

    /**
     * Map a same-local-day span onto {@code dummyDay} so Grafana can use a 24h x-axis.
     * End at local midnight becomes dummyDay+1 00:00. Spans shorter than {@code minVisualMin}
     * are stretched so a short drive is still a few pixels wide.
     */
    public static Instant[] clockRange(Instant start, Instant end, ZoneId zone, LocalDate dummyDay, int minVisualMin) {
        ZoneId z = zone == null ? ZoneId.of("UTC") : zone;
        LocalDate dummy = dummyDay == null ? LocalDate.EPOCH : dummyDay;
        LocalTime startTod = start.atZone(z).toLocalTime();
        Instant clockStart = dummy.atTime(startTod).atZone(z).toInstant();
        Instant clockEnd;
        LocalDate endDate = end.atZone(z).toLocalDate();
        LocalTime endTod = end.atZone(z).toLocalTime();
        if (endTod.equals(LocalTime.MIDNIGHT) && endDate.isAfter(start.atZone(z).toLocalDate())) {
            clockEnd = dummy.plusDays(1).atStartOfDay(z).toInstant();
        } else {
            clockEnd = dummy.atTime(endTod).atZone(z).toInstant();
        }
        if (!clockEnd.isAfter(clockStart)) {
            clockEnd = dummy.plusDays(1).atStartOfDay(z).toInstant();
        }
        int min = Math.max(minVisualMin, 0);
        if (min > 0 && Duration.between(clockStart, clockEnd).toMinutes() < min) {
            Instant stretched = clockStart.plusSeconds(min * 60L);
            Instant cap = dummy.plusDays(1).atStartOfDay(z).toInstant();
            clockEnd = stretched.isAfter(cap) ? cap : stretched;
        }
        return new Instant[]{clockStart, clockEnd};
    }

    private static List<ActivitySpan> splitOne(ActivitySpan span, ZoneId zone) {
        if (span == null || span.start() == null || span.end() == null || !span.end().isAfter(span.start())) {
            return span == null ? List.of() : List.of(span);
        }
        List<ActivitySpan> parts = new ArrayList<>();
        Instant cursor = span.start();
        Instant end = span.end();
        while (cursor.isBefore(end)) {
            Instant nextMidnight = cursor.atZone(zone).toLocalDate().plusDays(1).atStartOfDay(zone).toInstant();
            Instant stop = nextMidnight.isBefore(end) ? nextMidnight : end;
            double mins = Duration.between(cursor, stop).toMillis() / 60_000.0;
            parts.add(new ActivitySpan(
                    span.kind(), span.sourceId(), cursor, stop, round1(mins),
                    span.locationPositionId(), span.afterDriveId()));
            cursor = stop;
        }
        return parts;
    }

    private static LocalDate localDate(Instant instant, ZoneId zone) {
        ZoneId z = zone == null ? ZoneId.of("UTC") : zone;
        Instant t = instant == null ? Instant.EPOCH : instant;
        return t.atZone(z).toLocalDate();
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
