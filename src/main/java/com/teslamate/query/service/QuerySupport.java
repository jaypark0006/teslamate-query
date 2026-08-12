package com.teslamate.query.service;

import com.teslamate.query.config.QueryProperties;
import com.teslamate.query.domain.units.DisplayUnits;
import com.teslamate.query.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;

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
        if (page == null || page < 1) {
            return 1;
        }
        return Math.min(page, 1_000_000);
    }

    public int size(Integer size) {
        int s = size == null ? properties.getDefaultPageSize() : size;
        if (s < 1) {
            s = properties.getDefaultPageSize();
        }
        return Math.min(s, properties.getMaxPageSize());
    }

    public int offset(int page, int size) {
        long off = (long) (page - 1) * size;
        if (off > Integer.MAX_VALUE) {
            throw new BadRequestException("page too large");
        }
        return (int) off;
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
