package pl.matkmiec.backend.moon.dto;

import java.time.LocalTime;

public record MoonTimesDto(
        LocalTime moonrise,
        LocalTime moonset,
        LocalTime moonTransit,
        LocalTime moonUnderfoot,
        Double moonAltitudeDegrees,
        Double moonAzimuthDegrees
) {
}
