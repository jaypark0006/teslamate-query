package com.teslamate.query.entity;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

import java.math.BigDecimal;
import java.time.Instant;

/** Row of table {@value Table#NAME} (per-sample charge telemetry). */
public record ChargeEntity(
        @ColumnName(Table.ID) Long id,
        @ColumnName(Table.CHARGING_PROCESS_ID) Long chargingProcessId,
        @ColumnName(Table.DATE) Instant date,
        @ColumnName(Table.BATTERY_LEVEL) Integer batteryLevel,
        @ColumnName(Table.USABLE_BATTERY_LEVEL) Integer usableBatteryLevel,
        @ColumnName(Table.CHARGE_ENERGY_ADDED) BigDecimal chargeEnergyAdded,
        @ColumnName(Table.CHARGER_POWER) Integer chargerPower,
        @ColumnName(Table.CHARGER_VOLTAGE) Integer chargerVoltage,
        @ColumnName(Table.CHARGER_ACTUAL_CURRENT) Integer chargerActualCurrent,
        @ColumnName(Table.CHARGER_PHASES) Integer chargerPhases,
        @ColumnName(Table.FAST_CHARGER_PRESENT) Boolean fastChargerPresent,
        @ColumnName(Table.FAST_CHARGER_TYPE) String fastChargerType,
        @ColumnName(Table.IDEAL_BATTERY_RANGE_KM) BigDecimal idealBatteryRangeKm,
        @ColumnName(Table.RATED_BATTERY_RANGE_KM) BigDecimal ratedBatteryRangeKm,
        @ColumnName(Table.OUTSIDE_TEMP) BigDecimal outsideTemp,
        @ColumnName(Table.BATTERY_HEATER_ON) Boolean batteryHeaterOn
) {
    public static final class Table {
        public static final String NAME = "charges";
        public static final String ID = "id";
        public static final String CHARGING_PROCESS_ID = "charging_process_id";
        public static final String DATE = "date";
        public static final String BATTERY_LEVEL = "battery_level";
        public static final String USABLE_BATTERY_LEVEL = "usable_battery_level";
        public static final String CHARGE_ENERGY_ADDED = "charge_energy_added";
        public static final String CHARGER_POWER = "charger_power";
        public static final String CHARGER_VOLTAGE = "charger_voltage";
        public static final String CHARGER_ACTUAL_CURRENT = "charger_actual_current";
        public static final String CHARGER_PHASES = "charger_phases";
        public static final String FAST_CHARGER_PRESENT = "fast_charger_present";
        public static final String FAST_CHARGER_TYPE = "fast_charger_type";
        public static final String IDEAL_BATTERY_RANGE_KM = "ideal_battery_range_km";
        public static final String RATED_BATTERY_RANGE_KM = "rated_battery_range_km";
        public static final String OUTSIDE_TEMP = "outside_temp";
        public static final String BATTERY_HEATER_ON = "battery_heater_on";

                public static final String COLUMNS =
                "id, charging_process_id, date, battery_level, usable_battery_level, charge_energy_added, "
                + "charger_power, charger_voltage, charger_actual_current, charger_phases, "
                + "fast_charger_present, fast_charger_type, ideal_battery_range_km, rated_battery_range_km, "
                + "outside_temp, battery_heater_on";

        private Table() {}
    }
}
