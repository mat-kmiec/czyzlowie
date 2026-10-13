package pl.matkmiec.backend.moon.dto;

import java.time.LocalDate;
import java.util.List;

public record MoonMonthlyDto(
        int year,
        int month,
        LocationInfoDto location,
        List<MoonCalendarDayDto> days,
        LocalDate nextNewMoon,
        LocalDate nextFullMoon,
        int bestFishingDaysCount
) {
}
