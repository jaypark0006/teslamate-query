package com.teslamate.query.entity;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

/** Table: car_settings */
public record CarSettingsEntity(
        @ColumnName("id") Long id,
        @ColumnName("suspend_min") Integer suspendMin,
        @ColumnName("suspend_after_idle_min") Integer suspendAfterIdleMin,
        @ColumnName("req_not_unlocked") Boolean reqNotUnlocked,
        @ColumnName("free_supercharging") Boolean freeSupercharging,
        @ColumnName("use_streaming_api") Boolean useStreamingApi,
        @ColumnName("enabled") Boolean enabled,
        @ColumnName("lfp_battery") Boolean lfpBattery
) {}
