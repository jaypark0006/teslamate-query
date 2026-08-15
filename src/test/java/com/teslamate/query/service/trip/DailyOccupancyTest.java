package com.teslamate.query.service.trip;

import com.teslamate.query.dto.DailyOccupancyDto;
import com.teslamate.query.dto.TimelineItemDto;
import com.teslamate.query.dto.TimelineKind;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DailyOccupancyTest {

    @Test
    void sumsHoursByDayAndKind() {
        List<DailyOccupancyDto> out = DailyOccupancy.from(List.of(
                item(TimelineKind.DRIVE, "2026-07-25", 30),
                item(TimelineKind.PARK, "2026-07-25", 90),
                item(TimelineKind.DRIVE, "2026-07-26", 60)));
        assertEquals(2, out.size());
        assertEquals("2026-07-25", out.get(0).day());
        assertEquals(0.5, out.get(0).driveHours());
        assertEquals(1.5, out.get(0).parkHours());
        assertEquals(0.0, out.get(0).chargeHours());
        assertEquals(1.0, out.get(1).driveHours());
    }

    private static TimelineItemDto item(TimelineKind kind, String day, double minutes) {
        Instant start = Instant.parse("2026-07-25T00:00:00Z");
        return new TimelineItemDto(
                1, kind, 1L, start, start.plusSeconds((long) (minutes * 60)),
                minutes, kind.name(), "", "#000",
                null, null, null, null, null, null, null,
                day, 0, null, null);
    }
}
