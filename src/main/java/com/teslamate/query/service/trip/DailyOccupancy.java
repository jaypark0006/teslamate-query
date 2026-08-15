package com.teslamate.query.service.trip;

import com.teslamate.query.dto.DailyOccupancyDto;
import com.teslamate.query.dto.TimelineItemDto;
import com.teslamate.query.dto.TimelineKind;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Roll timeline rows up to hours-per-day for a stacked bar. */
public final class DailyOccupancy {

    private DailyOccupancy() {}

    public static List<DailyOccupancyDto> from(List<TimelineItemDto> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        Map<String, double[]> byDay = new LinkedHashMap<>();
        for (TimelineItemDto item : items) {
            if (item == null || item.day() == null || item.kind() == null) {
                continue;
            }
            double hours = hours(item);
            if (hours <= 0) {
                continue;
            }
            double[] bucket = byDay.computeIfAbsent(item.day(), k -> new double[3]);
            int i = switch (item.kind()) {
                case DRIVE -> 0;
                case CHARGE -> 1;
                case PARK -> 2;
            };
            bucket[i] += hours;
        }
        List<DailyOccupancyDto> out = new ArrayList<>(byDay.size());
        for (Map.Entry<String, double[]> e : byDay.entrySet()) {
            double[] b = e.getValue();
            out.add(new DailyOccupancyDto(e.getKey(), round2(b[0]), round2(b[1]), round2(b[2])));
        }
        return out;
    }

    static double hours(TimelineItemDto item) {
        if (item.durationMin() != null) {
            return item.durationMin() / 60.0;
        }
        if (item.start() != null && item.end() != null && item.end().isAfter(item.start())) {
            return Duration.between(item.start(), item.end()).toMillis() / 3_600_000.0;
        }
        return 0;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
