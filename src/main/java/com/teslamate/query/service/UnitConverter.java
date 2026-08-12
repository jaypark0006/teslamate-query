package com.teslamate.query.service;

import com.teslamate.query.domain.units.DisplayUnits;
import com.teslamate.query.domain.units.LengthUnit;
import com.teslamate.query.domain.units.TempUnit;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Converts metric DB values to display units (TeslaMate-compatible factors).
 * <ul>
 *   <li>km → mi: {@code / 1.60934}</li>
 *   <li>°C → °F: {@code * 9/5 + 32}</li>
 *   <li>m → ft: {@code * 3.28084}</li>
 * </ul>
 */
public final class UnitConverter {

    /** Same factor as TeslaMate {@code convert_km}. */
    public static final double KM_PER_MILE = 1.60934;
    public static final double FEET_PER_METER = 3.28084;

    private static final int SCALE = 6;

    private UnitConverter() {}

    /** km (DB) → display length unit. */
    public static Double length(Double km, DisplayUnits units) {
        if (km == null) {
            return null;
        }
        if (units.length() == LengthUnit.KM) {
            return km;
        }
        return round(km / KM_PER_MILE);
    }

    public static BigDecimal length(BigDecimal km, DisplayUnits units) {
        if (km == null) {
            return null;
        }
        if (units.length() == LengthUnit.KM) {
            return km;
        }
        return km.divide(BigDecimal.valueOf(KM_PER_MILE), SCALE, RoundingMode.HALF_UP);
    }

    /** °C (DB) → display temp unit. */
    public static Double temp(Double celsius, DisplayUnits units) {
        if (celsius == null) {
            return null;
        }
        if (units.temperature() == TempUnit.C) {
            return celsius;
        }
        return round(celsius * 9.0 / 5.0 + 32.0);
    }

    public static BigDecimal temp(BigDecimal celsius, DisplayUnits units) {
        if (celsius == null) {
            return null;
        }
        if (units.temperature() == TempUnit.C) {
            return celsius;
        }
        return celsius.multiply(BigDecimal.valueOf(9))
                .divide(BigDecimal.valueOf(5), SCALE, RoundingMode.HALF_UP)
                .add(BigDecimal.valueOf(32));
    }

    /** km/h (DB) → display speed unit. */
    public static Double speed(Double kmh, DisplayUnits units) {
        if (kmh == null) {
            return null;
        }
        if (units.length() == LengthUnit.KM) {
            return kmh;
        }
        return round(kmh / KM_PER_MILE);
    }

    public static Integer speed(Integer kmh, DisplayUnits units) {
        if (kmh == null) {
            return null;
        }
        if (units.length() == LengthUnit.KM) {
            return kmh;
        }
        return (int) Math.round(kmh / KM_PER_MILE);
    }

    /** metres (DB elevation / geofence radius) → display elevation unit. */
    public static Integer elevation(Integer meters, DisplayUnits units) {
        if (meters == null) {
            return null;
        }
        if (units.length() == LengthUnit.KM) {
            return meters;
        }
        return (int) Math.round(meters * FEET_PER_METER);
    }

    /**
     * Filter input length → km for SQL (DB is always km).
     * {@code minDistance} is interpreted in the request {@code lengthUnit}.
     */
    public static Double toKm(Double lengthInDisplayUnit, DisplayUnits units) {
        if (lengthInDisplayUnit == null) {
            return null;
        }
        if (units.length() == LengthUnit.KM) {
            return lengthInDisplayUnit;
        }
        return lengthInDisplayUnit * KM_PER_MILE;
    }

    private static double round(double v) {
        return BigDecimal.valueOf(v).setScale(SCALE, RoundingMode.HALF_UP).doubleValue();
    }
}
