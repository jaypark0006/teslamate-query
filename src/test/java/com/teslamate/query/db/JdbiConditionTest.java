package com.teslamate.query.db;

import com.teslamate.query.db.condition.ChargingProcessSearchCondition;
import com.teslamate.query.db.condition.DriveSearchCondition;
import org.junit.jupiter.api.Test;

import java.time.Instant;

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
        assertEquals(from, c.params().get("startDateFrom"));
        assertTrue(c.sortClause().contains("ORDER BY"));
    }

    @Test
    void nullFiltersOmitted() {
        var c = DriveSearchCondition.builder().carId(null).minDistance(null).build();
        assertEquals("", c.whereClause());
        assertTrue(c.params().isEmpty());
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
}
