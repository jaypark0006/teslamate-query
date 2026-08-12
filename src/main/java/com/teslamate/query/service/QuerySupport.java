package com.teslamate.query.service;

import com.teslamate.query.config.QueryProperties;
import com.teslamate.query.domain.units.DisplayUnits;
import com.teslamate.query.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Locale;

@Component
public class QuerySupport {

    private final QueryProperties properties;

    public QuerySupport(QueryProperties properties) {
        this.properties = properties;
    }

    /**
     * Parse display units from query params. Defaults: {@code km}, {@code C}.
     * Filter inputs such as {@code minDistance} are interpreted in {@code lengthUnit}
     * and converted to km for SQL.
     */
    public DisplayUnits units(String lengthUnit, String tempUnit) {
        return DisplayUnits.of(lengthUnit, tempUnit);
    }

    public int page(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    public int size(Integer size) {
        int s = size == null ? properties.getDefaultPageSize() : size;
        if (s < 1) {
            s = properties.getDefaultPageSize();
        }
        return Math.min(s, properties.getMaxPageSize());
    }

    public int offset(int page, int size) {
        return (page - 1) * size;
    }

    public String rangeMode(String range, String preferredFromSettings) {
        String r = (range != null && !range.isBlank()) ? range : preferredFromSettings;
        if (r == null || r.isBlank()) {
            r = "rated";
        }
        r = r.trim().toLowerCase(Locale.ROOT);
        if (!r.equals("ideal") && !r.equals("rated")) {
            throw new BadRequestException("range must be 'ideal' or 'rated'");
        }
        return r;
    }

    public Instant parseInstant(String value, String name) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (Exception e) {
            try {
                return OffsetDateTime.parse(value).toInstant();
            } catch (Exception e2) {
                throw new BadRequestException(name + " must be ISO-8601 datetime, got: " + value);
            }
        }
    }

    public Instant[] requireRange(String fromStr, String toStr) {
        Instant from = parseInstant(fromStr, "from");
        Instant to = parseInstant(toStr, "to");
        if (from == null || to == null) {
            throw new BadRequestException("from and to are required (ISO-8601)");
        }
        if (from.isAfter(to)) {
            throw new BadRequestException("from must be before to");
        }
        return new Instant[]{from, to};
    }
}
