package com.teslamate.query.db.condition;

import com.teslamate.query.db.JdbiCondition;

import java.time.Instant;

/** Single-table filters on {@code updates}. */
public class UpdateSearchCondition extends JdbiCondition {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends UpdateSearchCondition {
        public Builder carId(Long value) {
            eq("car_id", "carId", value);
            return this;
        }

        public Builder startDateFrom(Instant value) {
            gteUtc("start_date", "startDateFrom", value);
            return this;
        }

        public Builder startDateTo(Instant value) {
            lteUtc("start_date", "startDateTo", value);
            return this;
        }

        public UpdateSearchCondition build() {
            if (sortClause().isEmpty()) {
                orderBy("start_date", "DESC");
            }
            return this;
        }
    }
}
