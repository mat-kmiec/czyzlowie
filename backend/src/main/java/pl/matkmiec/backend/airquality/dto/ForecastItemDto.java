package pl.matkmiec.backend.airquality.dto;

import java.time.LocalDate;

public record ForecastItemDto(
        LocalDate date,
        Double value,
        String unit
) {}
