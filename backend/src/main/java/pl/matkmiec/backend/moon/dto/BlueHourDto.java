package pl.matkmiec.backend.moon.dto;

import java.time.LocalTime;

public record BlueHourDto(
        LocalTime start,
        LocalTime end
) {
}
