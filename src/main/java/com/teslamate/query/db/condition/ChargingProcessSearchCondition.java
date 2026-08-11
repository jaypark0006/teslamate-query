package com.teslamate.query.db.condition;

import com.teslamate.query.db.JdbiCondition;
import com.teslamate.query.entity.ChargingProcessEntity.Table;

import java.time.Instant;

/** Single-table filters for {@link Table#NAME} only (no join, no alias). */
public class ChargingProcessSearchCondition extends JdbiCondition {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends ChargingProcessSearchCondition {

        public Builder() {
            conditions.add("(" + Table.CHARGE_ENERGY_ADDED + " IS NULL OR "
                    + Table.CHARGE_ENERGY_ADDED + " > 0)");
        }

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

        public Builder geofenceId(Long value) {
            eq(Table.GEOFENCE_ID, "geofenceId", value);
            return this;
        }

        public Builder incompleteOnly(Boolean value) {
            rawNoParam(Table.END_DATE + " IS NULL", Boolean.TRUE.equals(value));
            return this;
        }

        public ChargingProcessSearchCondition build() {
            if (sortClause().isEmpty()) {
                orderBy(Table.START_DATE, "DESC");
            }
            return this;
        }
    }
}
