package com.teslamate.query.entity;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

/** Table: settings */
public record SettingsEntity(
        @ColumnName("id") Long id,
        @ColumnName("unit_of_length") String unitOfLength,
        @ColumnName("unit_of_temperature") String unitOfTemperature,
        @ColumnName("unit_of_pressure") String unitOfPressure,
        @ColumnName("preferred_range") String preferredRange,
        @ColumnName("base_url") String baseUrl,
        @ColumnName("grafana_url") String grafanaUrl,
        @ColumnName("language") String language
) {}
