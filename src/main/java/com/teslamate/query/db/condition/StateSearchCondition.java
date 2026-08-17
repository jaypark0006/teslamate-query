package com.teslamate.query.db.condition;

import com.teslamate.query.db.JdbiCondition;

import java.time.Instant;

/** Single-table filters on {@code states}. */
public class StateSearchCondition extends JdbiCondition {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends StateSearchCondition {
        public Builder carId(Long value) {
            eq("car_id", "carId", value);
            return this;
        }

        /** Interval overlaps [from, to]: start_date <= to AND (end_date IS NULL OR end_date >= from) */
        public Builder overlapping(Instant from, Instant to) {
            if (to != null) {
                lteUtc("start_date", "overlapTo", to);
            }
            if (from != null) {
                rawUtc("(end_date IS NULL OR end_date >= :overlapFrom)", "overlapFrom", from);
            }
            return this;
        }

        public StateSearchCondition build() {
            if (sortClause().isEmpty()) {
                orderBy("start_date", "ASC");
            }
            return this;
        }
    }
}
