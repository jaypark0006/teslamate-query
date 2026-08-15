package com.teslamate.query.db.condition;

import com.teslamate.query.db.JdbiCondition;

import java.time.Instant;

/** Single-table filters on {@code charging_processes} (no join, no alias). */
public class ChargingProcessSearchCondition extends JdbiCondition {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends ChargingProcessSearchCondition {

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

        public Builder geofenceId(Long value) {
            eq("geofence_id", "geofenceId", value);
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

        public Builder excludeZeroEnergy(Boolean value) {
            rawNoParam("(charge_energy_added IS NULL OR charge_energy_added > 0)",
                    Boolean.TRUE.equals(value));
            return this;
        }

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

        public ChargingProcessSearchCondition build() {
            if (sortClause().isEmpty()) {
                orderBy("start_date", "DESC");
            }
            return this;
        }
    }
}
