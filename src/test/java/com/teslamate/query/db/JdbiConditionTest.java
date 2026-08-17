package com.teslamate.query.db;

import com.teslamate.query.db.condition.ChargeSearchCondition;
import com.teslamate.query.db.condition.ChargingProcessSearchCondition;
import com.teslamate.query.db.condition.DriveSearchCondition;
import com.teslamate.query.db.condition.PositionSearchCondition;
import com.teslamate.query.db.condition.StateSearchCondition;
import com.teslamate.query.db.condition.UpdateSearchCondition;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JdbiConditionTest {

    @Test
    void driveWhereClauseAndParams() {
        Instant from = Instant.parse("2025-01-01T00:00:00Z");
        Instant to = Instant.parse("2025-02-01T00:00:00Z");
        var c = DriveSearchCondition.builder()
                .carId(1L)
                .startDateFrom(from)
                .startDateTo(to)
                .minDistance(1.0)
                .incompleteOnly(true)
                .build();

        assertTrue(c.whereClause().startsWith("WHERE "));
        assertTrue(c.whereClause().contains("car_id = :carId"));
        assertTrue(c.whereClause().contains("start_date >= :startDateFrom"));
        assertTrue(c.whereClause().contains("end_date IS NULL"));
        assertEquals(1L, c.params().get("carId"));
        assertEquals(LocalDateTime.parse("2025-01-01T00:00:00"), c.params().get("startDateFrom"));
        assertTrue(c.sortClause().contains("ORDER BY"));
    }

    @Test
    void nullFiltersOmitted() {
        var c = DriveSearchCondition.builder().carId(null).minDistance(null).build();
        assertEquals("", c.whereClause());
        assertTrue(c.params().isEmpty());
    }

    @Test
    void driveOverlapUsesEndDate() {
        Instant from = Instant.parse("2025-10-01T00:00:00Z");
        Instant to = Instant.parse("2025-10-08T00:00:00Z");
        var c = DriveSearchCondition.builder().carId(1L).overlapping(from, to).build();
        assertTrue(c.whereClause().contains("start_date <= :overlapTo"));
        assertTrue(c.whereClause().contains("end_date IS NULL OR end_date >= :overlapFrom"));
        assertEquals(LocalDateTime.parse("2025-10-01T00:00:00"), c.params().get("overlapFrom"));
        assertEquals(LocalDateTime.parse("2025-10-08T00:00:00"), c.params().get("overlapTo"));
    }

    @Test
    void everyTimestampConditionStoresDatabaseLocalDateTimes() {
        Instant from = Instant.parse("2026-08-17T12:45:06.254Z");
        Instant to = Instant.parse("2026-08-17T13:45:06.254Z");
        List<JdbiCondition> conditions = List.of(
                ChargeSearchCondition.builder().dateFrom(from).dateTo(to).build(),
                ChargingProcessSearchCondition.builder()
                        .startDateFrom(from).startDateTo(to).overlapping(from, to).build(),
                DriveSearchCondition.builder()
                        .startDateFrom(from).startDateTo(to).overlapping(from, to).build(),
                PositionSearchCondition.builder().dateFrom(from).dateTo(to).build(),
                StateSearchCondition.builder().overlapping(from, to).build(),
                UpdateSearchCondition.builder().startDateFrom(from).startDateTo(to).build());

        conditions.stream()
                .flatMap(condition -> condition.params().values().stream())
                .forEach(value -> assertInstanceOf(LocalDateTime.class, value));
    }

    @Test
    void chargingHasNoDefaultEnergyFilter() {
        var c = ChargingProcessSearchCondition.builder().carId(1L).build();
        assertFalse(c.whereClause().contains("charge_energy_added"));
        assertEquals(1L, c.params().get("carId"));
    }

    @Test
    void chargingExcludeZeroEnergyOptional() {
        var c = ChargingProcessSearchCondition.builder().excludeZeroEnergy(true).build();
        assertTrue(c.whereClause().contains("charge_energy_added"));
    }

    @Test
    void completedOnlyOrdersByEndDate() {
        var c = DriveSearchCondition.builder().carId(1L).completedOnly(true).newestEndFirst().build();
        assertTrue(c.whereClause().contains("end_date IS NOT NULL"));
        assertTrue(c.sortClause().contains("end_date DESC"));
    }
}
