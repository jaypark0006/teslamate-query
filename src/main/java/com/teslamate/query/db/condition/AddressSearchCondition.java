package com.teslamate.query.db.condition;

import com.teslamate.query.db.JdbiCondition;

/** Single-table filters on {@code addresses}. */
public class AddressSearchCondition extends JdbiCondition {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends AddressSearchCondition {

        public Builder city(String value) {
            eq("city", "city", value);
            return this;
        }

        public Builder country(String value) {
            eq("country", "country", value);
            return this;
        }

        public Builder osmId(Long value) {
            eq("osm_id", "osmId", value);
            return this;
        }

        public AddressSearchCondition build() {
            if (sortClause().isEmpty()) {
                orderBy("id", "ASC");
            }
            return this;
        }
    }
}
