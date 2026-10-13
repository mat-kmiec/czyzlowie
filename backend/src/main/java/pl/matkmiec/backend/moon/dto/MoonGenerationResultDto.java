package pl.matkmiec.backend.moon.dto;

public record MoonGenerationResultDto(
        int year,
        int generalDaysProcessed,
        int stationsProcessed,
        long stationDaysProcessed,
        long executionTimeMs,
        String status,
        String message
) {
}
