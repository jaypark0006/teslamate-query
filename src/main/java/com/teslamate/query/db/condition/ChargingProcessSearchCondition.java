package com.teslamate.query.db.condition;

import com.teslamate.query.db.JdbiCondition;

import java.time.Instant;

/** Dynamic filters for {@code charging_processes cp}. */
public class ChargingProcessSearchCondition extends JdbiCondition {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends ChargingProcessSearchCondition {

        public Builder() {
            // default business filter used by Grafana charges list
            conditions.add("(cp.charge_energy_added IS NULL OR cp.charge_energy_added > 0)");
        }

        public Builder carId(Long value) {
            eq("cp.car_id", "carId", value);
            return this;
        }

        public Builder startDateFrom(Instant value) {
            gte("cp.start_date", "startDateFrom", value);
            return this;
        }

        public Builder startDateTo(Instant value) {
            lte("cp.start_date", "startDateTo", value);
            return this;
        }

        public Builder geofenceId(Long value) {
            eq("cp.geofence_id", "geofenceId", value);
            return this;
        }

        public Builder incompleteOnly(Boolean value) {
            rawNoParam("cp.end_date IS NULL", Boolean.TRUE.equals(value));
            return this;
        }

        public Builder orderByStartDateDesc() {
            orderBy("cp.start_date", "DESC");
            return this;
        }

        public ChargingProcessSearchCondition build() {
            if (sortClause().isEmpty()) {
                orderByStartDateDesc();
            }
            return this;
        }
    }
}
