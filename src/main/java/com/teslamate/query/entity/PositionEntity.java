package com.teslamate.query.entity;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

import java.math.BigDecimal;
import java.time.Instant;

/** Table: positions */
public record PositionEntity(
        @ColumnName("id") Long id,
        @ColumnName("car_id") Long carId,
        @ColumnName("drive_id") Long driveId,
        @ColumnName("date") Instant date,
        @ColumnName("latitude") BigDecimal latitude,
        @ColumnName("longitude") BigDecimal longitude,
        @ColumnName("elevation") Integer elevation,
        @ColumnName("speed") Integer speed,
        @ColumnName("power") Integer power,
        @ColumnName("odometer") Double odometer,
        @ColumnName("ideal_battery_range_km") BigDecimal idealBatteryRangeKm,
        @ColumnName("est_battery_range_km") BigDecimal estBatteryRangeKm,
        @ColumnName("rated_battery_range_km") BigDecimal ratedBatteryRangeKm,
        @ColumnName("battery_level") Integer batteryLevel,
        @ColumnName("usable_battery_level") Integer usableBatteryLevel,
        @ColumnName("outside_temp") BigDecimal outsideTemp,
        @ColumnName("inside_temp") BigDecimal insideTemp,
        @ColumnName("fan_status") Integer fanStatus,
        @ColumnName("driver_temp_setting") BigDecimal driverTempSetting,
        @ColumnName("passenger_temp_setting") BigDecimal passengerTempSetting,
        @ColumnName("is_climate_on") Boolean climateOn,
        @ColumnName("is_rear_defroster_on") Boolean rearDefrosterOn,
        @ColumnName("is_front_defroster_on") Boolean frontDefrosterOn,
        @ColumnName("battery_heater") Boolean batteryHeater,
        @ColumnName("battery_heater_on") Boolean batteryHeaterOn,
        @ColumnName("tpms_pressure_fl") BigDecimal tpmsPressureFl,
        @ColumnName("tpms_pressure_fr") BigDecimal tpmsPressureFr,
        @ColumnName("tpms_pressure_rl") BigDecimal tpmsPressureRl,
        @ColumnName("tpms_pressure_rr") BigDecimal tpmsPressureRr
) {}
