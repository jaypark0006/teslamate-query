package com.teslamate.query.dto;

/** Full row from {@code car_settings} table. */
public record CarSettingsDto(
        Long id,
        Integer suspendMin,
        Integer suspendAfterIdleMin,
        Boolean reqNotUnlocked,
        Boolean freeSupercharging,
        Boolean useStreamingApi,
        Boolean enabled,
        Boolean lfpBattery
) {
}
