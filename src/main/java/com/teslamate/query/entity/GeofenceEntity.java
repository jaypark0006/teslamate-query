package com.teslamate.query.entity;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

import java.math.BigDecimal;

/** Table: geofences */
public record GeofenceEntity(
        @ColumnName("id") Long id,
        @ColumnName("name") String name,
        @ColumnName("latitude") BigDecimal latitude,
        @ColumnName("longitude") BigDecimal longitude,
        @ColumnName("radius") Integer radius,
        @ColumnName("billing_type") String billingType,
        @ColumnName("cost_per_unit") BigDecimal costPerUnit,
        @ColumnName("session_fee") BigDecimal sessionFee
) {}
