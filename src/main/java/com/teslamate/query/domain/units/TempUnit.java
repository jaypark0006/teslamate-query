package com.teslamate.query.domain.units;

import com.teslamate.query.exception.BadRequestException;

import java.util.Locale;

/** Display / request temperature unit. DB always stores Celsius. */
public enum TempUnit {
    C,
    F;

    public static TempUnit parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return C;
        }
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "c", "celsius", "°c" -> C;
            case "f", "fahrenheit", "°f" -> F;
            default -> throw new BadRequestException("tempUnit must be 'C' or 'F', got: " + raw);
        };
    }

    public String code() {
        return name();
    }
}
