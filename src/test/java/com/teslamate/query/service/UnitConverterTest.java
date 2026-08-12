package com.teslamate.query.service;

import com.teslamate.query.domain.units.DisplayUnits;
import com.teslamate.query.domain.units.LengthUnit;
import com.teslamate.query.domain.units.TempUnit;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class UnitConverterTest {

    private static final DisplayUnits MI_F = new DisplayUnits(LengthUnit.MI, TempUnit.F);
    private static final DisplayUnits KM_C = DisplayUnits.METRIC;

    @Test
    void lengthKmUnchanged() {
        assertEquals(100.0, UnitConverter.length(100.0, KM_C));
        assertEquals(0, BigDecimal.valueOf(100).compareTo(UnitConverter.length(BigDecimal.valueOf(100), KM_C)));
    }

    @Test
    void lengthToMiles() {
        double mi = UnitConverter.length(160.934, MI_F);
        assertEquals(100.0, mi, 0.001);
    }

    @Test
    void tempToFahrenheit() {
        assertEquals(32.0, UnitConverter.temp(0.0, MI_F), 0.001);
        assertEquals(212.0, UnitConverter.temp(100.0, MI_F), 0.001);
    }

    @Test
    void filterMinDistanceConvertedToKm() {
        assertEquals(16.0934, UnitConverter.toKm(10.0, MI_F), 0.0001);
        assertEquals(10.0, UnitConverter.toKm(10.0, KM_C));
    }

    @Test
    void parseUnits() {
        assertEquals(LengthUnit.MI, DisplayUnits.of("mi", "F").length());
        assertEquals(TempUnit.F, DisplayUnits.of("km", "fahrenheit").temperature());
        assertTrue(DisplayUnits.of(null, null).isMetric());
    }
}
