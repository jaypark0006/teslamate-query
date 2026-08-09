package com.teslamate.query.service;

import com.teslamate.query.config.QueryProperties;
import com.teslamate.query.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;

@Component
public class QuerySupport {

    private final QueryProperties properties;

    public QuerySupport(QueryProperties properties) {
        this.properties = properties;
    }

    public int page(Integer page) {
        if (page == null || page < 1) {
            return 1;
        }
        return page;
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
        String r = range != null ? range : preferredFromSettings;
        if (r == null || r.isBlank()) {
            r = "ideal";
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

    public void requireTimeRange(Instant from, Instant to) {
        if (from == null || to == null) {
            throw new BadRequestException("from and to are required (ISO-8601)");
        }
        if (from.isAfter(to)) {
            throw new BadRequestException("from must be before to");
        }
    }

    public OffsetDateTime toOffset(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }

    /** Address display matching Grafana: COALESCE(geofence, name/road+house, city) */
    public static final String ADDRESS_SQL = """
            COALESCE(%1$s.name, CONCAT_WS(', ',
              COALESCE(%2$s.name, NULLIF(CONCAT_WS(' ', %2$s.road, %2$s.house_number), '')),
              %2$s.city))
            """;
}
