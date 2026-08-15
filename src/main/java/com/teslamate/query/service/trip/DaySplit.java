package com.teslamate.query.service.trip;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

/** Local-day labels and a 24h clock mapping for the Grafana axis. */
public final class DaySplit {

    private DaySplit() {}

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
        LocalDate startDate = start.atZone(z).toLocalDate();
        LocalTime startTod = start.atZone(z).toLocalTime();
        Instant clockStart = dummy.atTime(startTod).atZone(z).toInstant();
        Instant clockEnd;
        LocalDate endDate = end.atZone(z).toLocalDate();
        LocalTime endTod = end.atZone(z).toLocalTime();
        if (endDate.isAfter(startDate)) {
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

    private static LocalDate localDate(Instant instant, ZoneId zone) {
        ZoneId z = zone == null ? ZoneId.of("UTC") : zone;
        Instant t = instant == null ? Instant.EPOCH : instant;
        return t.atZone(z).toLocalDate();
    }
}
