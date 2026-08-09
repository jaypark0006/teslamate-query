package com.teslamate.query.dto;

import java.util.List;

public record LocationStatsDto(
        Long carId,
        List<Place> places
) {
    public record Place(
            String name,
            String city,
            String country,
            long visitCount,
            double totalDistanceKm,
            String kind
    ) {
    }
}
