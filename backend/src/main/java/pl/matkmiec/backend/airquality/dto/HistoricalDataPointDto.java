package pl.matkmiec.backend.airquality.dto;

import java.time.LocalDateTime;

public record HistoricalDataPointDto(
        LocalDateTime timestamp,
        Double value
) {}
