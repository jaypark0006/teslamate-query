package com.teslamate.query.domain.units;

import com.teslamate.query.exception.BadRequestException;

import java.util.Locale;

/** Display / request length unit. DB always stores kilometres. */
public enum LengthUnit {
    KM,
    MI;

    public static LengthUnit parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return KM;
        }
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "km", "kilometer", "kilometers", "kilometre", "kilometres" -> KM;
            case "mi", "mile", "miles" -> MI;
            default -> throw new BadRequestException("lengthUnit must be 'km' or 'mi', got: " + raw);
        };
    }

    public String code() {
        return name().toLowerCase(Locale.ROOT);
    }
}
