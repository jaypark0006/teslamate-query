package com.teslamate.query.entity;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Row of table {@value Table#NAME}.
 */
public record DriveEntity(
        @ColumnName(Table.ID) Long id,
        @ColumnName(Table.CAR_ID) Long carId,
        @ColumnName(Table.START_DATE) Instant startDate,
        @ColumnName(Table.END_DATE) Instant endDate,
        @ColumnName(Table.OUTSIDE_TEMP_AVG) BigDecimal outsideTempAvg,
        @ColumnName(Table.INSIDE_TEMP_AVG) BigDecimal insideTempAvg,
        @ColumnName(Table.SPEED_MAX) Integer speedMax,
        @ColumnName(Table.POWER_MAX) Integer powerMax,
        @ColumnName(Table.POWER_MIN) Integer powerMin,
        @ColumnName(Table.START_IDEAL_RANGE_KM) BigDecimal startIdealRangeKm,
        @ColumnName(Table.END_IDEAL_RANGE_KM) BigDecimal endIdealRangeKm,
        @ColumnName(Table.START_RATED_RANGE_KM) BigDecimal startRatedRangeKm,
        @ColumnName(Table.END_RATED_RANGE_KM) BigDecimal endRatedRangeKm,
        @ColumnName(Table.START_KM) Double startKm,
        @ColumnName(Table.END_KM) Double endKm,
        @ColumnName(Table.DISTANCE) Double distance,
        @ColumnName(Table.DURATION_MIN) Integer durationMin,
        @ColumnName(Table.ASCENT) Integer ascent,
        @ColumnName(Table.DESCENT) Integer descent,
        @ColumnName(Table.START_POSITION_ID) Long startPositionId,
        @ColumnName(Table.END_POSITION_ID) Long endPositionId,
        @ColumnName(Table.START_ADDRESS_ID) Long startAddressId,
        @ColumnName(Table.END_ADDRESS_ID) Long endAddressId,
        @ColumnName(Table.START_GEOFENCE_ID) Long startGeofenceId,
        @ColumnName(Table.END_GEOFENCE_ID) Long endGeofenceId
) {
    /** Physical table {@code drives}. */
    public static final class Table {
        public static final String NAME = "drives";
        public static final String ID = "id";
        public static final String CAR_ID = "car_id";
        public static final String START_DATE = "start_date";
        public static final String END_DATE = "end_date";
        public static final String OUTSIDE_TEMP_AVG = "outside_temp_avg";
        public static final String INSIDE_TEMP_AVG = "inside_temp_avg";
        public static final String SPEED_MAX = "speed_max";
        public static final String POWER_MAX = "power_max";
        public static final String POWER_MIN = "power_min";
        public static final String START_IDEAL_RANGE_KM = "start_ideal_range_km";
        public static final String END_IDEAL_RANGE_KM = "end_ideal_range_km";
        public static final String START_RATED_RANGE_KM = "start_rated_range_km";
        public static final String END_RATED_RANGE_KM = "end_rated_range_km";
        public static final String START_KM = "start_km";
        public static final String END_KM = "end_km";
        public static final String DISTANCE = "distance";
        public static final String DURATION_MIN = "duration_min";
        public static final String ASCENT = "ascent";
        public static final String DESCENT = "descent";
        public static final String START_POSITION_ID = "start_position_id";
        public static final String END_POSITION_ID = "end_position_id";
        public static final String START_ADDRESS_ID = "start_address_id";
        public static final String END_ADDRESS_ID = "end_address_id";
        public static final String START_GEOFENCE_ID = "start_geofence_id";
        public static final String END_GEOFENCE_ID = "end_geofence_id";

        /** All columns for SELECT list (no table alias). */
                public static final String COLUMNS =
                "id, car_id, start_date, end_date, outside_temp_avg, inside_temp_avg, "
                + "speed_max, power_max, power_min, "
                + "start_ideal_range_km, end_ideal_range_km, start_rated_range_km, end_rated_range_km, "
                + "start_km, end_km, distance, duration_min, ascent, descent, "
                + "start_position_id, end_position_id, start_address_id, end_address_id, "
                + "start_geofence_id, end_geofence_id";

        private Table() {}
    }
}
