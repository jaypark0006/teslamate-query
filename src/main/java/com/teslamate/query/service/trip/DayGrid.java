package com.teslamate.query.service.trip;

import com.teslamate.query.dto.DayGridCellDto;
import com.teslamate.query.dto.TimelineItemDto;
import com.teslamate.query.dto.TimelineKind;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Paint each activity onto 15-minute slots of local days.
 * Overnight parks fill every day they cover (grid only; the log stays one row).
 */
public final class DayGrid {

    static final int SLOT_MIN = 15;
    static final int SLOTS_PER_DAY = 24 * 60 / SLOT_MIN;

    private DayGrid() {}

    public static List<DayGridCellDto> paintFromTimeline(List<TimelineItemDto> items, ZoneId zone) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        List<ActivitySpan> spans = new ArrayList<>(items.size());
        for (TimelineItemDto item : items) {
            if (item == null || item.start() == null || item.end() == null) {
                continue;
            }
            double min = item.durationMin() == null ? 0 : item.durationMin();
            spans.add(new ActivitySpan(item.kind(), item.id(), item.start(), item.end(), min, null, null));
        }
        return paint(spans, zone);
    }

    public static List<DayGridCellDto> paint(List<ActivitySpan> spans, ZoneId zone) {
        if (spans == null || spans.isEmpty()) {
            return List.of();
        }
        ZoneId z = zone == null ? ZoneId.of("UTC") : zone;
        Map<String, int[]> cells = new LinkedHashMap<>();
        for (ActivitySpan span : spans) {
            if (span == null || span.start() == null || span.end() == null || !span.end().isAfter(span.start())) {
                continue;
            }
            int code = code(span.kind());
            Instant cursor = span.start();
            Instant end = span.end();
            int guard = 0;
            while (cursor.isBefore(end) && guard++ < 20_000) {
                ZonedDateTime local = cursor.atZone(z);
                LocalDate day = local.toLocalDate();
                int minuteOfDay = local.getHour() * 60 + local.getMinute();
                int slot = Math.min(minuteOfDay / SLOT_MIN, SLOTS_PER_DAY - 1);
                Instant slotEnd = day.atStartOfDay(z).plusMinutes((long) (slot + 1) * SLOT_MIN).toInstant();
                if (slotEnd.isAfter(end)) {
                    slotEnd = end;
                }
                if (!slotEnd.isAfter(cursor)) {
                    slotEnd = cursor.plusSeconds(60);
                }
                String key = day + "#" + slot;
                int[] cell = cells.get(key);
                if (cell == null || preferred(code, cell[0])) {
                    cells.put(key, new int[]{code, day.getYear(), day.getMonthValue(), day.getDayOfMonth(), slot});
                }
                cursor = slotEnd;
            }
        }
        List<DayGridCellDto> out = new ArrayList<>(cells.size());
        for (int[] c : cells.values()) {
            LocalDate day = LocalDate.of(c[1], c[2], c[3]);
            Instant midnight = day.atStartOfDay(z).toInstant();
            int minutes = c[4] * SLOT_MIN;
            double hour = minutes / 60.0;
            String slot = String.format("%02d:%02d", minutes / 60, minutes % 60);
            String kind = switch (c[0]) {
                case DayGridCellDto.DRIVE -> "DRIVE";
                case DayGridCellDto.CHARGE -> "CHARGE";
                default -> "PARK";
            };
            out.add(new DayGridCellDto(midnight, day.toString(), hour, slot, c[0], kind));
        }
        out.sort(Comparator.comparing(DayGridCellDto::day).thenComparingDouble(DayGridCellDto::hour));
        return out;
    }

    private static int code(TimelineKind kind) {
        if (kind == TimelineKind.DRIVE) {
            return DayGridCellDto.DRIVE;
        }
        if (kind == TimelineKind.CHARGE) {
            return DayGridCellDto.CHARGE;
        }
        return DayGridCellDto.PARK;
    }

    /** Charge covers drive covers park when two things share a 15-minute slot. */
    private static boolean preferred(int incoming, int existing) {
        return incoming > existing;
    }
}
