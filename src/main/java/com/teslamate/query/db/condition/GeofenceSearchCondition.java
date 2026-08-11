package com.teslamate.query.db.condition;

import com.teslamate.query.db.JdbiCondition;

/** Single-table filters on {@code geofences}. */
public class GeofenceSearchCondition extends JdbiCondition {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends GeofenceSearchCondition {

        public Builder name(String value) {
            eq("name", "name", value);
            return this;
        }

        public GeofenceSearchCondition build() {
            if (sortClause().isEmpty()) {
                orderBy("name", "ASC");
            }
            return this;
        }
    }
}
