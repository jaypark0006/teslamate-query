package com.teslamate.query.entity;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

import java.math.BigDecimal;
import java.time.Instant;

/** Table: charges */
public record ChargeEntity(
        @ColumnName("id") Long id,
        @ColumnName("charging_process_id") Long chargingProcessId,
        @ColumnName("date") Instant date,
        @ColumnName("battery_level") Integer batteryLevel,
        @ColumnName("usable_battery_level") Integer usableBatteryLevel,
        @ColumnName("charge_energy_added") BigDecimal chargeEnergyAdded,
        @ColumnName("charger_power") Integer chargerPower,
        @ColumnName("charger_voltage") Integer chargerVoltage,
        @ColumnName("charger_actual_current") Integer chargerActualCurrent,
        @ColumnName("charger_phases") Integer chargerPhases,
        @ColumnName("fast_charger_present") Boolean fastChargerPresent,
        @ColumnName("fast_charger_type") String fastChargerType,
        @ColumnName("ideal_battery_range_km") BigDecimal idealBatteryRangeKm,
        @ColumnName("rated_battery_range_km") BigDecimal ratedBatteryRangeKm,
        @ColumnName("outside_temp") BigDecimal outsideTemp,
        @ColumnName("battery_heater_on") Boolean batteryHeaterOn
) {}
