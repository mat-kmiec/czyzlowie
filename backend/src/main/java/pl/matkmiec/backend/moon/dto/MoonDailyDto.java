package pl.matkmiec.backend.moon.dto;

import java.time.LocalDate;

public record MoonDailyDto(
        LocalDate date,
        LocationInfoDto location,
        GeneralMoonDataDto generalMoon,
        SunTimesDto sunTimes,
        MoonTimesDto moonTimes,
        SolunarDayDto solunar
) {
}
