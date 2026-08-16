package com.teslamate.query.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/** One chronological DRIVE / CHARGE / PARK row for the map timeline log. */
public record TimelineItemDto(
        int seq,
        TimelineKind kind,
        Long id,
        Instant start,
        Instant end,
        Double durationMin,
        String title,
        String detail,
        String color,
        Double latitude,
        Double longitude,
        Double distanceKm,
        Integer startSocPercent,
        Integer endSocPercent,
        Double energyKwh,
        String chargeType,
        String day,
        Integer dayBand,
        Instant clockStart,
        Instant clockEnd,
        Integer highlight
) {
    public TimelineItemDto withHighlight(int value) {
        return new TimelineItemDto(seq, kind, id, start, end, durationMin, title, detail, color,
                latitude, longitude, distanceKm, startSocPercent, endSocPercent, energyKwh, chargeType,
                day, dayBand, clockStart, clockEnd, value);
    }

    /** Epoch millis for Grafana data links. Field :date:seconds is the clock second, not unix time. */
    @JsonProperty("startMs")
    public Long startMs() {
        return start == null ? null : start.toEpochMilli();
    }

    @JsonProperty("endMs")
    public Long endMs() {
        return end == null ? null : end.toEpochMilli();
    }
}
