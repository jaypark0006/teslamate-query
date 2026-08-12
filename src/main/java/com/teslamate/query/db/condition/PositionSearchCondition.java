package com.teslamate.query.db.condition;

import com.teslamate.query.db.JdbiCondition;

import java.time.Instant;

/** Single-table filters on {@code positions}. */
public class PositionSearchCondition extends JdbiCondition {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends PositionSearchCondition {
        public Builder carId(Long value) {
            eq("car_id", "carId", value);
            return this;
        }

        public Builder driveId(Long value) {
            eq("drive_id", "driveId", value);
            return this;
        }

        public Builder dateFrom(Instant value) {
            gte("date", "dateFrom", value);
            return this;
        }

        public Builder dateTo(Instant value) {
            lte("date", "dateTo", value);
            return this;
        }

        public Builder cleanOnly(Boolean value) {
            rawNoParam("ideal_battery_range_km IS NOT NULL", Boolean.TRUE.equals(value));
            return this;
        }

        public Builder newestFirst() {
            orderBy("date", "DESC");
            return this;
        }

        public PositionSearchCondition build() {
            if (sortClause().isEmpty()) {
                orderBy("date", "ASC");
            }
            return this;
        }
    }
}
