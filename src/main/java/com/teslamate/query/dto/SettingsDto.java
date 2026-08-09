package com.teslamate.query.dto;

public record SettingsDto(
        Long id,
        String unitOfLength,
        String unitOfTemperature,
        String unitOfPressure,
        String preferredRange,
        String baseUrl,
        String grafanaUrl,
        String language,
        String themeMode
) {
}
