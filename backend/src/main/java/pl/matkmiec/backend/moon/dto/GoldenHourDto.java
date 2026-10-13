package pl.matkmiec.backend.moon.dto;

import java.time.LocalTime;

public record GoldenHourDto(
        LocalTime start,
        LocalTime end,
        String description
) {
}
