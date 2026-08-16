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
        assertTrue(cells.stream().anyMatch(c -> "22".equals(c.slot())));
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
        var driveCell = cells.stream().filter(c -> "00".equals(c.slot())).findFirst().orElseThrow();
        assertEquals(DayGridCellDto.DRIVE, driveCell.kindCode());
        assertEquals(1L, driveCell.sourceId());
        assertEquals("Drive #1", driveCell.label());
        assertEquals(DayGridCellDto.hoverCode(DayGridCellDto.DRIVE, 1), driveCell.hoverCode());
    }

    @Test
    void timelineDetailShowsOnDriveCellHoverLabel() {
        var item = new TimelineItemDto(
                1, TimelineKind.DRIVE, 5155L,
                Instant.parse("2026-08-13T00:10:00Z"), Instant.parse("2026-08-13T00:11:00Z"),
                1.0, "Home → Work", "0.0 km · 1 min · 90% → 90%", "#3b82f6",
                null, null, 0.0, 90, 90, null, null,
                "2026-08-13", 0, null, null, 0);
        List<DayGridCellDto> cells = DayGrid.paintFromTimeline(List.of(item), ZoneId.of("UTC"));
        DayGridCellDto cell = cells.stream().filter(c -> "00".equals(c.slot())).findFirst().orElseThrow();
        assertEquals("Drive #5155 · 0.0 km · 1 min · 90% → 90%", cell.label());
        assertEquals("0.0 km · 1 min · 90% → 90%", cell.detail());
        assertEquals(DayGridCellDto.hoverCode(DayGridCellDto.DRIVE, 5155), cell.hoverCode());
    }

    @Test
    void chargeAndDriveInSameHourKeepChargeColorAndBothIds() {
        ActivitySpan drive = new ActivitySpan(
                TimelineKind.DRIVE, 12L,
                Instant.parse("2026-08-10T16:00:00Z"),
                Instant.parse("2026-08-10T16:25:00Z"),
                25, null, null);
        ActivitySpan charge = new ActivitySpan(
                TimelineKind.CHARGE, 9L,
                Instant.parse("2026-08-10T16:25:00Z"),
                Instant.parse("2026-08-10T16:50:00Z"),
                25, null, null);
        List<DayGridCellDto> cells = DayGrid.paint(List.of(drive, charge), ZoneId.of("UTC"));
        DayGridCellDto cell = cells.stream().filter(c -> "16".equals(c.slot())).findFirst().orElseThrow();
        assertEquals(DayGridCellDto.CHARGE, cell.kindCode());
        assertEquals(9L, cell.sourceId());
        assertEquals("Charge #9  |  Drive #12", cell.label());
    }

    @Test
    void chargeAndDriveHoverKeepsEachKindWithItsOwnDetail() {
        var drive = new TimelineItemDto(
                1, TimelineKind.DRIVE, 2254L,
                Instant.parse("2026-08-10T16:00:00Z"), Instant.parse("2026-08-10T16:25:00Z"),
                25.0, "Home → Work", "106.6 km · 58 min · 100% → 56%", "#3b82f6",
                null, null, 106.6, 100, 56, null, null,
                "2026-08-10", 0, null, null, 0);
        var charge = new TimelineItemDto(
                2, TimelineKind.CHARGE, 200L,
                Instant.parse("2026-08-10T16:25:00Z"), Instant.parse("2026-08-10T16:50:00Z"),
                25.0, "DC charge", "35.9 kWh · 39% → 100% · 38 min", "#22c55e",
                null, null, null, 39, 100, 35.9, "DC",
                "2026-08-10", 0, null, null, 0);
        List<DayGridCellDto> cells = DayGrid.paintFromTimeline(List.of(drive, charge), ZoneId.of("UTC"));
        DayGridCellDto cell = cells.stream().filter(c -> "16".equals(c.slot())).findFirst().orElseThrow();
        assertEquals(
                "Charge #200 · 35.9 kWh · 39% → 100% · 38 min  |  Drive #2254 · 106.6 km · 58 min · 100% → 56%",
                cell.label());
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
        assertTrue(cells.stream().anyMatch(c -> c.day().equals("2026-08-10") && "·02".equals(c.slot())));
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
                "2026-08-10", 0, null, null, 0);
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
        assertEquals("04", keys.getFirst());
        assertEquals("10", keys.get(6));
        assertEquals("·00", keys.get(20));
        assertEquals("·03", keys.get(23));
        assertEquals(10, DayGrid.parseClockHour("10"));
        assertEquals(0, DayGrid.parseClockHour("·00"));
        assertEquals(2, DayGrid.parseClockHour("·02"));
        assertEquals(null, DayGrid.parseClockHour("★"));
        assertEquals(18432L, DayGrid.parseSourceId("Drive #18432"));
        assertEquals(99L, DayGrid.parseSourceId("Charge #99"));
        assertEquals(null, DayGrid.parseSourceId("Park"));
    }

    @Test
    void last24hDoesNotDropTheEveningColumn() {
        Instant from = Instant.parse("2026-08-12T14:15:28Z");
        Instant to = Instant.parse("2026-08-13T14:15:28Z");
        ActivitySpan park = new ActivitySpan(
                TimelineKind.PARK, null, from.minusSeconds(3600), to.minusSeconds(60),
                24 * 60, null, null);
        List<DayGridCellDto> cells = DayGrid.paint(List.of(park), SH, 4, from, to);
        assertTrue(cells.stream().allMatch(c -> !c.time().isBefore(from) && c.time().isBefore(to)));
        List<String> slots = cells.stream()
                .map(DayGridCellDto::slot)
                .filter(s -> !s.contains("★"))
                .distinct()
                .sorted()
                .toList();
        assertEquals("04", slots.getFirst());
        assertTrue(slots.contains("·03"));
        assertTrue(cells.stream().anyMatch(c -> c.day().equals("2026-08-12")));
        assertTrue(cells.stream().anyMatch(c -> c.day().equals("2026-08-13")));
    }

    @Test
    void shanghaiSevenAmDriveIsLocalHour07OnThatCivilDay() {
        // 07:10–07:40 Asia/Shanghai = 23:10–23:40 UTC previous evening
        ActivitySpan drive = new ActivitySpan(
                TimelineKind.DRIVE, 1L,
                Instant.parse("2026-08-12T23:10:00Z"),
                Instant.parse("2026-08-12T23:40:00Z"),
                30, null, null);
        List<DayGridCellDto> cells = DayGrid.paint(List.of(drive), SH, 4);
        assertTrue(cells.stream().anyMatch(c ->
                c.day().equals("2026-08-13")
                        && "07".equals(c.slot())
                        && c.kindCode() == DayGridCellDto.DRIVE));
        Instant x = cells.stream().filter(c -> "07".equals(c.slot())).findFirst().orElseThrow().time();
        assertEquals("2026-08-13", x.atZone(SH).toLocalDate().toString());
    }

    @Test
    void highlightMarksOverlappingDriveHours() {
        ActivitySpan drive = new ActivitySpan(
                TimelineKind.DRIVE, 12L,
                Instant.parse("2026-08-13T00:00:00Z"),
                Instant.parse("2026-08-13T02:00:00Z"),
                120, null, null);
        List<DayGridCellDto> cells = DayGrid.paint(List.of(drive), ZoneId.of("UTC"), 0,
                Instant.parse("2026-08-13T00:00:00Z"), Instant.parse("2026-08-13T04:00:00Z"));
        Instant from = Instant.parse("2026-08-13T00:00:00Z");
        Instant to = Instant.parse("2026-08-13T02:00:00Z");
        List<DayGridCellDto> marked = DayGrid.applyHighlight(cells, from, to, TimelineKind.DRIVE, 0, ZoneId.of("UTC"));
        assertTrue(marked.stream().anyMatch(c ->
                "00".equals(c.slot()) && c.kindCode() == DayGridCellDto.HIGHLIGHT_DRIVE));
        assertTrue(marked.stream().anyMatch(c ->
                "01".equals(c.slot()) && c.kindCode() == DayGridCellDto.HIGHLIGHT_DRIVE));
        assertTrue(marked.stream().anyMatch(c ->
                "03".equals(c.slot()) && c.kindCode() == 0));
    }
}
