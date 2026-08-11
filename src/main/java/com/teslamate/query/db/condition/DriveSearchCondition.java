package com.teslamate.query.db.condition;

import com.teslamate.query.db.JdbiCondition;
import com.teslamate.query.entity.DriveEntity.Table;

import java.time.Instant;

/**
 * Single-table filters for {@link Table#NAME} only (no join, no alias).
 */
public class DriveSearchCondition extends JdbiCondition {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends DriveSearchCondition {

        public Builder carId(Long value) {
            eq(Table.CAR_ID, "carId", value);
            return this;
        }

        public Builder startDateFrom(Instant value) {
            gte(Table.START_DATE, "startDateFrom", value);
            return this;
        }

        public Builder startDateTo(Instant value) {
            lte(Table.START_DATE, "startDateTo", value);
            return this;
        }

        public Builder minDistance(Double value) {
            gte(Table.DISTANCE, "minDistance", value);
            return this;
        }

        public Builder minDuration(Integer value) {
            gte(Table.DURATION_MIN, "minDuration", value);
            return this;
        }

        public Builder geofenceId(Long value) {
            if (value == null) {
                return this;
            }
            conditions.add("(" + Table.START_GEOFENCE_ID + " = :geofenceId OR "
                    + Table.END_GEOFENCE_ID + " = :geofenceId)");
            params.put("geofenceId", value);
            return this;
        }

        public Builder incompleteOnly(Boolean value) {
            rawNoParam(Table.END_DATE + " IS NULL", Boolean.TRUE.equals(value));
            return this;
        }

        public DriveSearchCondition build() {
            if (sortClause().isEmpty()) {
                orderBy(Table.START_DATE, "DESC");
            }
            return this;
        }
    }
}
