package com.teslamate.query.db.condition;

import com.teslamate.query.db.JdbiCondition;

import java.time.Instant;

/** Dynamic filters for {@code drives d}. */
public class DriveSearchCondition extends JdbiCondition {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends DriveSearchCondition {

        public Builder carId(Long value) {
            eq("d.car_id", "carId", value);
            return this;
        }

        public Builder startDateFrom(Instant value) {
            gte("d.start_date", "startDateFrom", value);
            return this;
        }

        public Builder startDateTo(Instant value) {
            lte("d.start_date", "startDateTo", value);
            return this;
        }

        public Builder minDistance(Double value) {
            gte("d.distance", "minDistance", value);
            return this;
        }

        public Builder minDuration(Integer value) {
            gte("d.duration_min", "minDuration", value);
            return this;
        }

        public Builder geofenceId(Long value) {
            if (value == null) {
                return this;
            }
            conditions.add("(d.start_geofence_id = :geofenceId OR d.end_geofence_id = :geofenceId)");
            params.put("geofenceId", value);
            return this;
        }

        public Builder incompleteOnly(Boolean value) {
            rawNoParam("d.end_date IS NULL", Boolean.TRUE.equals(value));
            return this;
        }

        public Builder orderByStartDateDesc() {
            orderBy("d.start_date", "DESC");
            return this;
        }

        public DriveSearchCondition build() {
            if (sortClause().isEmpty()) {
                orderByStartDateDesc();
            }
            return this;
        }
    }
}
