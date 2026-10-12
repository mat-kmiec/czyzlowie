package pl.matkmiec.backend.airquality.dto;

import java.time.LocalDateTime;

public record PollutantMeasurementDto(
        String paramCode,
        String paramName,
        Double value,
        String unit,
        LocalDateTime measurementTime,
        String indexLevel
) {}
