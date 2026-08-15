package com.teslamate.query.service.trip;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
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
