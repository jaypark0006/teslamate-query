package com.teslamate.query.entity;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

/** Row of table {@value Table#NAME}. */
public record SettingsEntity(
        @ColumnName(Table.ID) Long id,
        @ColumnName(Table.UNIT_OF_LENGTH) String unitOfLength,
        @ColumnName(Table.UNIT_OF_TEMPERATURE) String unitOfTemperature,
        @ColumnName(Table.UNIT_OF_PRESSURE) String unitOfPressure,
        @ColumnName(Table.PREFERRED_RANGE) String preferredRange,
        @ColumnName(Table.BASE_URL) String baseUrl,
        @ColumnName(Table.GRAFANA_URL) String grafanaUrl,
        @ColumnName(Table.LANGUAGE) String language
) {
    public static final class Table {
        public static final String NAME = "settings";
        public static final String ID = "id";
        public static final String UNIT_OF_LENGTH = "unit_of_length";
        public static final String UNIT_OF_TEMPERATURE = "unit_of_temperature";
        public static final String UNIT_OF_PRESSURE = "unit_of_pressure";
        public static final String PREFERRED_RANGE = "preferred_range";
        public static final String BASE_URL = "base_url";
        public static final String GRAFANA_URL = "grafana_url";
        public static final String LANGUAGE = "language";
                public static final String COLUMNS =
                "id, unit_of_length, unit_of_temperature, unit_of_pressure, preferred_range, "
                + "base_url, grafana_url, language";
        private Table() {}
    }
}
