package com.teslamate.query.domain.units;

import com.teslamate.query.dto.UnitsMeta;

/**
 * Requested display units for API responses.
 * <p>
 * DB rows are always metric (km, °C, km/h). Conversion happens after load.
 */
public record DisplayUnits(LengthUnit length, TempUnit temperature) {

    public static final DisplayUnits METRIC = new DisplayUnits(LengthUnit.KM, TempUnit.C);

    public static DisplayUnits of(String lengthUnit, String tempUnit) {
        return new DisplayUnits(LengthUnit.parse(lengthUnit), TempUnit.parse(tempUnit));
    }

    public boolean isMetric() {
        return length == LengthUnit.KM && temperature == TempUnit.C;
    }

    public UnitsMeta toMeta() {
        return new UnitsMeta(
                length.code(),
                temperature.code(),
                length == LengthUnit.MI ? "mph" : "km/h",
                length == LengthUnit.MI ? "ft" : "m"
        );
    }
}
