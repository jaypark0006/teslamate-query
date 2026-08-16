package com.teslamate.query.service.trip;

import com.teslamate.query.dto.DayGridCellDto;
import com.teslamate.query.dto.TimelineItemDto;
import com.teslamate.query.dto.TimelineKind;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DayGridTest {

    private static final ZoneId SH = ZoneId.of("Asia/Shanghai");

    @Test
    void overnightParkFillsBothLocalDays() {
        // 22:00 Aug 10 CST → 08:00 Aug 11 CST
        ActivitySpan park = new ActivitySpan(
                TimelineKind.PARK, null,
                Instant.parse("2026-08-10T14:00:00Z"),
                Instant.parse("2026-08-11T00:00:00Z"),
                600, null, null);
        List<DayGridCellDto> cells = DayGrid.paint(List.of(park), SH);
        assertTrue(cells.stream().anyMatch(c -> c.day().equals("2026-08-10") && c.hour() >= 22));
        assertTrue(cells.stream().anyMatch(c -> c.day().equals("2026-08-11") && c.hour() < 8));
        assertTrue(cells.stream().allMatch(c -> c.kindCode() == DayGridCellDto.PARK));
        assertTrue(cells.stream().anyMatch(c -> "22 22:00".equals(c.slot())));
    }

    @Test
    void driveWinsOverParkInTheSameSlot() {
        ActivitySpan park = new ActivitySpan(
                TimelineKind.PARK, null,
                Instant.parse("2026-08-10T00:00:00Z"),
                Instant.parse("2026-08-10T01:00:00Z"),
                60, null, null);
        ActivitySpan drive = new ActivitySpan(
                TimelineKind.DRIVE, 1L,
                Instant.parse("2026-08-10T00:10:00Z"),
                Instant.parse("2026-08-10T00:20:00Z"),
                10, null, null);
        List<DayGridCellDto> cells = DayGrid.paint(List.of(park, drive), ZoneId.of("UTC"));
        boolean sawDrive = cells.stream().anyMatch(c ->
                c.kindCode() == DayGridCellDto.DRIVE && c.hour() < 1);
        assertTrue(sawDrive);
        var driveCell = cells.stream().filter(c -> "00 00:00".equals(c.slot())).findFirst().orElseThrow();
        assertEquals(DayGridCellDto.DRIVE, driveCell.kindCode());
        assertEquals(1L, driveCell.sourceId());
        assertEquals("Drive #1", driveCell.label());
    }

    @Test
    void fourAmDayKeepsPredawnOnPreviousColumn() {
        // 02:00–04:00 Aug 11 CST belongs to the Aug 10 04:00–04:00 block
        ActivitySpan park = new ActivitySpan(
                TimelineKind.PARK, null,
                Instant.parse("2026-08-10T18:00:00Z"),
                Instant.parse("2026-08-10T20:00:00Z"),
                120, null, null);
        List<DayGridCellDto> cells = DayGrid.paint(List.of(park), SH, 4);
        assertTrue(cells.stream().anyMatch(c -> c.day().equals("2026-08-10") && "22 02:00".equals(c.slot())));
        assertTrue(cells.stream().noneMatch(c -> c.day().equals("2026-08-11")));
    }

    @Test
    void saturdayGetsWeekendMark() {
        ActivitySpan park = new ActivitySpan(
                TimelineKind.PARK, null,
                Instant.parse("2026-08-15T04:00:00Z"),
                Instant.parse("2026-08-15T06:00:00Z"),
                120, null, null);
        List<DayGridCellDto> cells = DayGrid.paint(List.of(park), SH, 4);
        assertTrue(cells.stream().anyMatch(c ->
                c.kindCode() == DayGridCellDto.WEEKEND && c.day().equals("2026-08-15")));
    }

    @Test
    void timelineOvernightParkStillFillsBothDays() {
        var item = new TimelineItemDto(
                1, TimelineKind.PARK, null,
                Instant.parse("2026-08-10T14:00:00Z"), Instant.parse("2026-08-11T00:00:00Z"),
                600.0, "Parked", "10h", "#1d4ed8",
                null, null, null, null, null, null, null,
                "2026-08-10", 0, null, null);
        List<DayGridCellDto> cells = DayGrid.paintFromTimeline(List.of(item), SH);
        assertTrue(cells.stream().anyMatch(c -> c.day().equals("2026-08-10") && c.hour() >= 22));
        assertTrue(cells.stream().anyMatch(c -> c.day().equals("2026-08-11") && c.hour() < 8));
    }

    @Test
    void fourAmSlotKeysSortWithFourOClockFirst() {
        List<String> keys = new java.util.ArrayList<>();
        for (int i = 0; i < 24; i++) {
            keys.add(DayGrid.slotKey(4, i));
        }
        List<String> sorted = new java.util.ArrayList<>(keys);
        sorted.sort(String::compareTo);
        assertEquals(keys, sorted);
        assertEquals("00 04:00", keys.getFirst());
        assertEquals("06 10:00", keys.get(6));
        assertEquals("20 00:00", keys.get(20));
        assertEquals("23 03:00", keys.get(23));
        assertEquals(10, DayGrid.parseClockHour("06 10:00"));
        assertEquals(0, DayGrid.parseClockHour("00:00"));
        assertEquals(null, DayGrid.parseClockHour("24 ★"));
        assertEquals(18432L, DayGrid.parseSourceId("Drive #18432"));
        assertEquals(99L, DayGrid.parseSourceId("Charge #99"));
        assertEquals(null, DayGrid.parseSourceId("Park"));
    }
}
