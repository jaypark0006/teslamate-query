package com.teslamate.query.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

/**
 * Typed highlight / map-focus selection. Built at the HTTP edge from Grafana vars.
 * {@code kind == null} means any kind.
 */
public record TripFocus(
        TimelineKind kind,
        Long id,
        Instant from,
        Instant to,
        LocalDate day,
        Integer clockHour
) {
    public static final TripFocus NONE = new TripFocus(null, null, null, null, null, null);

    public boolean isEmpty() {
        return id == null && from == null && to == null && day == null && clockHour == null;
    }

    public boolean matches(TimelineKind itemKind) {
        return kind == null || kind == itemKind;
    }

    public Set<TimelineKind> layersOrDefault(Set<TimelineKind> requested) {
        if (requested != null && !requested.isEmpty() && requested.size() < TimelineKind.values().length) {
            return requested;
        }
        return kind == null ? Set.of(TimelineKind.values()) : Set.of(kind);
    }
}