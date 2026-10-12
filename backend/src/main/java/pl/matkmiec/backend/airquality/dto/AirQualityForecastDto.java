package pl.matkmiec.backend.airquality.dto;

import java.util.List;

public record AirQualityForecastDto(
        String terytCode,
        String pollutant,
        List<ForecastItemDto> items
) {}
