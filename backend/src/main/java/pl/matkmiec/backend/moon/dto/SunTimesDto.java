package pl.matkmiec.backend.moon.dto;

import java.time.LocalTime;

public record SunTimesDto(
        LocalTime sunrise,
        LocalTime sunset,
        LocalTime solarNoon,
        String dayLength,
        long dayLengthSeconds,
        Long dayLengthDiffSeconds,
        GoldenHourDto morningGoldenHour,
        GoldenHourDto eveningGoldenHour,
        BlueHourDto morningBlueHour,
        BlueHourDto eveningBlueHour,
        TwilightDto civilTwilight,
        TwilightDto nauticalTwilight,
        TwilightDto astronomicalTwilight
) {
}
