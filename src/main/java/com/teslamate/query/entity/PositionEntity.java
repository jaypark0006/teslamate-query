package com.teslamate.query.entity;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

import java.math.BigDecimal;
import java.time.Instant;

/** Row of table {@value Table#NAME}. */
public record PositionEntity(
        @ColumnName(Table.ID) Long id,
        @ColumnName(Table.CAR_ID) Long carId,
        @ColumnName(Table.DRIVE_ID) Long driveId,
        @ColumnName(Table.DATE) Instant date,
        @ColumnName(Table.LATITUDE) BigDecimal latitude,
        @ColumnName(Table.LONGITUDE) BigDecimal longitude,
        @ColumnName(Table.ELEVATION) Integer elevation,
        @ColumnName(Table.SPEED) Integer speed,
        @ColumnName(Table.POWER) Integer power,
        @ColumnName(Table.ODOMETER) Double odometer,
        @ColumnName(Table.IDEAL_BATTERY_RANGE_KM) BigDecimal idealBatteryRangeKm,
        @ColumnName(Table.EST_BATTERY_RANGE_KM) BigDecimal estBatteryRangeKm,
        @ColumnName(Table.RATED_BATTERY_RANGE_KM) BigDecimal ratedBatteryRangeKm,
        @ColumnName(Table.BATTERY_LEVEL) Integer batteryLevel,
        @ColumnName(Table.USABLE_BATTERY_LEVEL) Integer usableBatteryLevel,
        @ColumnName(Table.OUTSIDE_TEMP) BigDecimal outsideTemp,
        @ColumnName(Table.INSIDE_TEMP) BigDecimal insideTemp
) {
    public static final class Table {
        public static final String NAME = "positions";
        public static final String ID = "id";
        public static final String CAR_ID = "car_id";
        public static final String DRIVE_ID = "drive_id";
        public static final String DATE = "date";
        public static final String LATITUDE = "latitude";
        public static final String LONGITUDE = "longitude";
        public static final String ELEVATION = "elevation";
        public static final String SPEED = "speed";
        public static final String POWER = "power";
        public static final String ODOMETER = "odometer";
        public static final String IDEAL_BATTERY_RANGE_KM = "ideal_battery_range_km";
        public static final String EST_BATTERY_RANGE_KM = "est_battery_range_km";
        public static final String RATED_BATTERY_RANGE_KM = "rated_battery_range_km";
        public static final String BATTERY_LEVEL = "battery_level";
        public static final String USABLE_BATTERY_LEVEL = "usable_battery_level";
        public static final String OUTSIDE_TEMP = "outside_temp";
        public static final String INSIDE_TEMP = "inside_temp";

                public static final String COLUMNS =
                "id, car_id, drive_id, date, latitude, longitude, elevation, speed, power, odometer, "
                + "ideal_battery_range_km, est_battery_range_km, rated_battery_range_km, "
                + "battery_level, usable_battery_level, outside_temp, inside_temp";

        private Table() {}
    }
}
