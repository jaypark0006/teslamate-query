package com.teslamate.query.entity;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

/** Row of table {@value Table#NAME}. */
public record CarEntity(
        @ColumnName(Table.ID) Long id,
        @ColumnName(Table.EID) Long eid,
        @ColumnName(Table.VID) Long vid,
        @ColumnName(Table.VIN) String vin,
        @ColumnName(Table.COL_NAME) String name,
        @ColumnName(Table.MODEL) String model,
        @ColumnName(Table.EFFICIENCY) Double efficiency,
        @ColumnName(Table.TRIM_BADGING) String trimBadging,
        @ColumnName(Table.MARKETING_NAME) String marketingName,
        @ColumnName(Table.EXTERIOR_COLOR) String exteriorColor,
        @ColumnName(Table.WHEEL_TYPE) String wheelType,
        @ColumnName(Table.SPOILER_TYPE) String spoilerType,
        @ColumnName(Table.DISPLAY_PRIORITY) Integer displayPriority,
        @ColumnName(Table.SETTINGS_ID) Long settingsId
) {
    public static final class Table {
        public static final String NAME = "cars";
        public static final String ID = "id";
        public static final String EID = "eid";
        public static final String VID = "vid";
        public static final String VIN = "vin";
        public static final String COL_NAME = "name";
        public static final String MODEL = "model";
        public static final String EFFICIENCY = "efficiency";
        public static final String TRIM_BADGING = "trim_badging";
        public static final String MARKETING_NAME = "marketing_name";
        public static final String EXTERIOR_COLOR = "exterior_color";
        public static final String WHEEL_TYPE = "wheel_type";
        public static final String SPOILER_TYPE = "spoiler_type";
        public static final String DISPLAY_PRIORITY = "display_priority";
        public static final String SETTINGS_ID = "settings_id";
                public static final String COLUMNS =
                "id, eid, vid, vin, name, model, efficiency, trim_badging, marketing_name, "
                + "exterior_color, wheel_type, spoiler_type, display_priority, settings_id";
        private Table() {}
    }
}
