package com.teslamate.query.entity;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

import java.math.BigDecimal;

/** Table: addresses */
public record AddressEntity(
        @ColumnName("id") Long id,
        @ColumnName("display_name") String displayName,
        @ColumnName("name") String name,
        @ColumnName("house_number") String houseNumber,
        @ColumnName("road") String road,
        @ColumnName("neighbourhood") String neighbourhood,
        @ColumnName("city") String city,
        @ColumnName("county") String county,
        @ColumnName("postcode") String postcode,
        @ColumnName("state") String state,
        @ColumnName("state_district") String stateDistrict,
        @ColumnName("country") String country,
        @ColumnName("latitude") BigDecimal latitude,
        @ColumnName("longitude") BigDecimal longitude,
        @ColumnName("osm_id") Long osmId,
        @ColumnName("osm_type") String osmType
) {}
