package com.teslamate.query.dto;

public record CarDto(
        Long carId,
        String name,
        String vin,
        String model,
        String marketingName,
        String trimBadging,
        Double efficiency,
        Integer displayPriority,
        String exteriorColor,
        String wheelType,
        Boolean lfpBattery,
        Boolean freeSupercharging,
        Boolean enabled
) {
}
