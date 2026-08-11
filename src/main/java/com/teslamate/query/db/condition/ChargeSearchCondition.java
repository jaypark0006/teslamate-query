package com.teslamate.query.db.condition;

import com.teslamate.query.db.JdbiCondition;

import java.time.Instant;

/** Single-table filters on {@code charges}. */
public class ChargeSearchCondition extends JdbiCondition {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends ChargeSearchCondition {
        public Builder chargingProcessId(Long value) {
            eq("charging_process_id", "chargingProcessId", value);
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

        public ChargeSearchCondition build() {
            if (sortClause().isEmpty()) {
                orderBy("date", "ASC");
            }
            return this;
        }
    }
}
