package pl.matkmiec.backend.moon.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record MoonCalendarDayDto(
        LocalDate date,
        MoonPhaseDto phase,
        double illuminationPercentage,
        double moonAgeDays,
        LocalTime sunrise,
        LocalTime sunset,
        LocalTime moonrise,
        LocalTime moonset,
        SolunarActivityLevel solunarRating,
        int solunarStars,
        int solunarScore,
        boolean isPerigee,
        boolean isApogee
) {
}
