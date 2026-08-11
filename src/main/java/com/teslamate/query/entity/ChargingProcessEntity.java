package com.teslamate.query.entity;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

import java.math.BigDecimal;
import java.time.Instant;

/** Row of table {@value Table#NAME}. */
public record ChargingProcessEntity(
        @ColumnName(Table.ID) Long id,
        @ColumnName(Table.CAR_ID) Long carId,
        @ColumnName(Table.START_DATE) Instant startDate,
        @ColumnName(Table.END_DATE) Instant endDate,
        @ColumnName(Table.CHARGE_ENERGY_ADDED) BigDecimal chargeEnergyAdded,
        @ColumnName(Table.CHARGE_ENERGY_USED) BigDecimal chargeEnergyUsed,
        @ColumnName(Table.START_IDEAL_RANGE_KM) BigDecimal startIdealRangeKm,
        @ColumnName(Table.END_IDEAL_RANGE_KM) BigDecimal endIdealRangeKm,
        @ColumnName(Table.START_RATED_RANGE_KM) BigDecimal startRatedRangeKm,
        @ColumnName(Table.END_RATED_RANGE_KM) BigDecimal endRatedRangeKm,
        @ColumnName(Table.START_BATTERY_LEVEL) Integer startBatteryLevel,
        @ColumnName(Table.END_BATTERY_LEVEL) Integer endBatteryLevel,
        @ColumnName(Table.DURATION_MIN) Integer durationMin,
        @ColumnName(Table.OUTSIDE_TEMP_AVG) BigDecimal outsideTempAvg,
        @ColumnName(Table.COST) BigDecimal cost,
        @ColumnName(Table.POSITION_ID) Long positionId,
        @ColumnName(Table.ADDRESS_ID) Long addressId,
        @ColumnName(Table.GEOFENCE_ID) Long geofenceId
) {
    public static final class Table {
        public static final String NAME = "charging_processes";
        public static final String ID = "id";
        public static final String CAR_ID = "car_id";
        public static final String START_DATE = "start_date";
        public static final String END_DATE = "end_date";
        public static final String CHARGE_ENERGY_ADDED = "charge_energy_added";
        public static final String CHARGE_ENERGY_USED = "charge_energy_used";
        public static final String START_IDEAL_RANGE_KM = "start_ideal_range_km";
        public static final String END_IDEAL_RANGE_KM = "end_ideal_range_km";
        public static final String START_RATED_RANGE_KM = "start_rated_range_km";
        public static final String END_RATED_RANGE_KM = "end_rated_range_km";
        public static final String START_BATTERY_LEVEL = "start_battery_level";
        public static final String END_BATTERY_LEVEL = "end_battery_level";
        public static final String DURATION_MIN = "duration_min";
        public static final String OUTSIDE_TEMP_AVG = "outside_temp_avg";
        public static final String COST = "cost";
        public static final String POSITION_ID = "position_id";
        public static final String ADDRESS_ID = "address_id";
        public static final String GEOFENCE_ID = "geofence_id";

                public static final String COLUMNS =
                "id, car_id, start_date, end_date, charge_energy_added, charge_energy_used, "
                + "start_ideal_range_km, end_ideal_range_km, start_rated_range_km, end_rated_range_km, "
                + "start_battery_level, end_battery_level, duration_min, outside_temp_avg, cost, "
                + "position_id, address_id, geofence_id";

        private Table() {}
    }
}
