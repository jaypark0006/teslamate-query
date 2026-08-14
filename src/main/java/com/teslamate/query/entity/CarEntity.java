package com.teslamate.query.entity;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

/** Table: cars */
public record CarEntity(
        @ColumnName("id") Long id,
        @ColumnName("eid") Long eid,
        @ColumnName("vid") Long vid,
        @ColumnName("vin") String vin,
        @ColumnName("name") String name,
        @ColumnName("model") String model,
        @ColumnName("efficiency") Double efficiency,
        @ColumnName("trim_badging") String trimBadging,
        @ColumnName("marketing_name") String marketingName,
        @ColumnName("exterior_color") String exteriorColor,
        @ColumnName("wheel_type") String wheelType,
        @ColumnName("spoiler_type") String spoilerType,
        @ColumnName("display_priority") Integer displayPriority,
        @ColumnName("settings_id") Long settingsId
) {}
