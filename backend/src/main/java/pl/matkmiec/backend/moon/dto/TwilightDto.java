package pl.matkmiec.backend.moon.dto;

import java.time.LocalTime;

public record TwilightDto(
        LocalTime dawn,
        LocalTime dusk
) {
}
