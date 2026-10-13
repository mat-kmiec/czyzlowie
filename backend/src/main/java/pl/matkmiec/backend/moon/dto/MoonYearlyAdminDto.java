package pl.matkmiec.backend.moon.dto;

import java.util.List;
import java.util.Map;

public record MoonYearlyAdminDto(
        int year,
        LocationInfoDto location,
        int totalDays,
        List<MoonCalendarDayDto> days,
        Map<String, Object> summaryStats
) {
}
