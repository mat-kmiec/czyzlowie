package pl.matkmiec.backend.moon.dto;

import java.time.LocalTime;

public record SolunarPeriodDto(
        SolunarPeriodType type,
        LocalTime startTime,
        LocalTime endTime,
        String name,
        String description,
        int activityRatingPercent
) {
}
