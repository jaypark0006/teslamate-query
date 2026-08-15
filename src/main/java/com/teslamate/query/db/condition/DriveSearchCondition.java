package com.teslamate.query.db.condition;

import com.teslamate.query.db.JdbiCondition;

import java.time.Instant;

/** Single-table filters on {@code drives} (no join, no alias). */
public class DriveSearchCondition extends JdbiCondition {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends DriveSearchCondition {

        public Builder carId(Long value) {
            eq("car_id", "carId", value);
            return this;
        }

        public Builder startDateFrom(Instant value) {
            gte("start_date", "startDateFrom", value);
            return this;
        }

        public Builder startDateTo(Instant value) {
            lte("start_date", "startDateTo", value);
            return this;
        }

        public Builder minDistance(Double value) {
            gte("distance", "minDistance", value);
            return this;
        }

        public Builder minDuration(Integer value) {
            gte("duration_min", "minDuration", value);
            return this;
        }

        public Builder geofenceId(Long value) {
            if (value == null) {
                return this;
            }
            conditions.add("(start_geofence_id = :geofenceId OR end_geofence_id = :geofenceId)");
            params.put("geofenceId", value);
            return this;
        }

        public Builder incompleteOnly(Boolean value) {
            rawNoParam("end_date IS NULL", Boolean.TRUE.equals(value));
            return this;
        }

        public Builder completedOnly(Boolean value) {
            rawNoParam("end_date IS NOT NULL", Boolean.TRUE.equals(value));
            return this;
        }

        public Builder newestEndFirst() {
            orderBy("end_date", "DESC");
            return this;
        }

        /** Interval overlaps [from, to]: start_date <= to AND (end_date IS NULL OR end_date >= from). */
        public Builder overlapping(Instant from, Instant to) {
            if (to != null) {
                lte("start_date", "overlapTo", to);
            }
            if (from != null) {
                conditions.add("(end_date IS NULL OR end_date >= :overlapFrom)");
                params.put("overlapFrom", from);
            }
            return this;
        }

        public Builder oldestFirst() {
            orderBy("start_date", "ASC");
            return this;
        }

        public DriveSearchCondition build() {
            if (sortClause().isEmpty()) {
                orderBy("start_date", "DESC");
            }
            return this;
        }
    }
}
