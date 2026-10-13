package pl.matkmiec.backend.moon.calculator;

import pl.matkmiec.backend.moon.dto.*;

import java.time.*;
import java.time.temporal.ChronoUnit;

public final class SolarCalculator {

    private static final double SUN_RISE_SET_ALTITUDE = -0.8333; // 50 arcminutes (refraction + solar radius)
    private static final double GOLDEN_HOUR_LOW_ALTITUDE = -4.0;
    private static final double GOLDEN_HOUR_HIGH_ALTITUDE = 6.0;
    private static final double BLUE_HOUR_LOW_ALTITUDE = -6.0;
    private static final double CIVIL_TWILIGHT_ALTITUDE = -6.0;
    private static final double NAUTICAL_TWILIGHT_ALTITUDE = -12.0;
    private static final double ASTRONOMICAL_TWILIGHT_ALTITUDE = -18.0;

    private SolarCalculator() {
    }

    public static SunTimesDto calculateSunTimes(double latitude, double longitude, LocalDate date, ZoneId zoneId) {
        ZoneId zone = (zoneId != null) ? zoneId : ZoneId.of("Europe/Warsaw");

        // Solar noon and solar coordinates for the given date
        SolarParameters params = calculateSolarParameters(date, longitude);

        LocalTime solarNoon = utHoursToLocalTime(date, params.solarNoonUtHours(), zone);
        LocalTime sunrise = calculateSunEventTime(latitude, params, SUN_RISE_SET_ALTITUDE, true, date, zone);
        LocalTime sunset = calculateSunEventTime(latitude, params, SUN_RISE_SET_ALTITUDE, false, date, zone);

        // Day length calculation
        long dayLengthSeconds = 0;
        String dayLengthFormatted = "00:00:00";
        if (sunrise != null && sunset != null) {
            dayLengthSeconds = ChronoUnit.SECONDS.between(sunrise, sunset);
            if (dayLengthSeconds < 0) {
                dayLengthSeconds += 86400;
            }
            long hours = dayLengthSeconds / 3600;
            long minutes = (dayLengthSeconds % 3600) / 60;
            long seconds = dayLengthSeconds % 60;
            dayLengthFormatted = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        }

        // Difference from previous day
        Long dayLengthDiffSeconds = calculateDayLengthDiff(latitude, longitude, date, dayLengthSeconds, zone);

        // Golden hours
        LocalTime morningGoldenStart = calculateSunEventTime(latitude, params, GOLDEN_HOUR_LOW_ALTITUDE, true, date, zone);
        LocalTime morningGoldenEnd = calculateSunEventTime(latitude, params, GOLDEN_HOUR_HIGH_ALTITUDE, true, date, zone);
        GoldenHourDto morningGoldenHour = new GoldenHourDto(
                morningGoldenStart,
                morningGoldenEnd,
                "Poranna złota godzina (kąt Słońca: -4° do +6°), miękkie ciepłe światło idealne do fotografii i żerowania drapieżników"
        );

        LocalTime eveningGoldenStart = calculateSunEventTime(latitude, params, GOLDEN_HOUR_HIGH_ALTITUDE, false, date, zone);
        LocalTime eveningGoldenEnd = calculateSunEventTime(latitude, params, GOLDEN_HOUR_LOW_ALTITUDE, false, date, zone);
        GoldenHourDto eveningGoldenHour = new GoldenHourDto(
                eveningGoldenStart,
                eveningGoldenEnd,
                "Wieczorna złota godzina (kąt Słońca: +6° do -4°), ciepłe światło i wzmożona aktywność ryb przed zmierzchem"
        );

        // Blue hours
        LocalTime morningBlueStart = calculateSunEventTime(latitude, params, BLUE_HOUR_LOW_ALTITUDE, true, date, zone);
        LocalTime morningBlueEnd = morningGoldenStart;
        BlueHourDto morningBlueHour = new BlueHourDto(morningBlueStart, morningBlueEnd);

        LocalTime eveningBlueStart = eveningGoldenEnd;
        LocalTime eveningBlueEnd = calculateSunEventTime(latitude, params, BLUE_HOUR_LOW_ALTITUDE, false, date, zone);
        BlueHourDto eveningBlueHour = new BlueHourDto(eveningBlueStart, eveningBlueEnd);

        // Twilights
        TwilightDto civilTwilight = new TwilightDto(
                calculateSunEventTime(latitude, params, CIVIL_TWILIGHT_ALTITUDE, true, date, zone),
                calculateSunEventTime(latitude, params, CIVIL_TWILIGHT_ALTITUDE, false, date, zone)
        );

        TwilightDto nauticalTwilight = new TwilightDto(
                calculateSunEventTime(latitude, params, NAUTICAL_TWILIGHT_ALTITUDE, true, date, zone),
                calculateSunEventTime(latitude, params, NAUTICAL_TWILIGHT_ALTITUDE, false, date, zone)
        );

        TwilightDto astronomicalTwilight = new TwilightDto(
                calculateSunEventTime(latitude, params, ASTRONOMICAL_TWILIGHT_ALTITUDE, true, date, zone),
                calculateSunEventTime(latitude, params, ASTRONOMICAL_TWILIGHT_ALTITUDE, false, date, zone)
        );

        return new SunTimesDto(
                sunrise,
                sunset,
                solarNoon,
                dayLengthFormatted,
                dayLengthSeconds,
                dayLengthDiffSeconds,
                morningGoldenHour,
                eveningGoldenHour,
                morningBlueHour,
                eveningBlueHour,
                civilTwilight,
                nauticalTwilight,
                astronomicalTwilight
        );
    }

    private static Long calculateDayLengthDiff(double latitude, double longitude, LocalDate date, long currentDayLengthSeconds, ZoneId zone) {
        LocalDate prevDate = date.minusDays(1);
        SolarParameters prevParams = calculateSolarParameters(prevDate, longitude);
        LocalTime prevSunrise = calculateSunEventTime(latitude, prevParams, SUN_RISE_SET_ALTITUDE, true, prevDate, zone);
        LocalTime prevSunset = calculateSunEventTime(latitude, prevParams, SUN_RISE_SET_ALTITUDE, false, prevDate, zone);
        if (prevSunrise != null && prevSunset != null) {
            long prevDayLengthSeconds = ChronoUnit.SECONDS.between(prevSunrise, prevSunset);
            if (prevDayLengthSeconds < 0) {
                prevDayLengthSeconds += 86400;
            }
            return currentDayLengthSeconds - prevDayLengthSeconds;
        }
        return 0L;
    }

    private static LocalTime calculateSunEventTime(double latitude, SolarParameters params, double altitudeDeg, boolean isRise, LocalDate date, ZoneId zone) {
        double latRad = AstroMathUtils.rad(latitude);
        double decRad = AstroMathUtils.rad(params.declinationDeg());
        double altRad = AstroMathUtils.rad(altitudeDeg);

        double cosHourAngle = (Math.sin(altRad) - Math.sin(latRad) * Math.sin(decRad)) / (Math.cos(latRad) * Math.cos(decRad));

        if (cosHourAngle > 1.0 || cosHourAngle < -1.0) {
            return null; // Sun never reaches this altitude on this day
        }

        double hourAngleDeg = AstroMathUtils.deg(Math.acos(cosHourAngle));
        double deltaHours = hourAngleDeg / 15.0;

        double eventUtHours = isRise ? (params.solarNoonUtHours() - deltaHours) : (params.solarNoonUtHours() + deltaHours);
        return utHoursToLocalTime(date, eventUtHours, zone);
    }

    public static SolarParameters calculateSolarParameters(LocalDate date, double longitude) {
        double jd = AstroMathUtils.toJulianDate(date, 12.0); // Noon UTC approx
        double t = AstroMathUtils.toJulianCenturies(jd);

        // Geometric Mean Longitude of Sun (degrees)
        double l0 = AstroMathUtils.normalizeDeg(280.46646 + 36000.76983 * t + 0.0003032 * t * t);

        // Mean Anomaly of Sun (degrees)
        double m = AstroMathUtils.normalizeDeg(357.52911 + 35999.05029 * t - 0.0001537 * t * t);

        // Eccentricity of Earth Orbit
        double e = 0.016708634 - 0.000042037 * t - 0.0000001267 * t * t;

        // Equation of Center (degrees)
        double c = (1.914602 - 0.004817 * t - 0.000014 * t * t) * AstroMathUtils.sinDeg(m)
                + (0.019993 - 0.000101 * t) * AstroMathUtils.sinDeg(2 * m)
                + 0.000289 * AstroMathUtils.sinDeg(3 * m);

        // Sun's True Longitude (degrees)
        double trueLong = l0 + c;

        // Sun's Apparent Longitude (degrees)
        double apparentLong = trueLong - 0.00569 - 0.00478 * AstroMathUtils.sinDeg(125.04 - 1934.136 * t);

        // Mean Obliquity of Ecliptic (degrees)
        double eps0 = 23.0 + (26.0 + (21.448 - 46.8150 * t - 0.00059 * t * t + 0.001813 * t * t * t) / 60.0) / 60.0;
        double eps = eps0 + 0.00256 * AstroMathUtils.cosDeg(125.04 - 1934.136 * t);

        // Declination (degrees)
        double declinationDeg = AstroMathUtils.asinDeg(AstroMathUtils.sinDeg(eps) * AstroMathUtils.sinDeg(apparentLong));

        // Equation of Time (minutes)
        double y = Math.pow(Math.tan(AstroMathUtils.rad(eps) / 2.0), 2);
        double eotMinutes = 4.0 * AstroMathUtils.deg(
                y * AstroMathUtils.sinDeg(2 * l0)
                        - 2 * e * AstroMathUtils.sinDeg(m)
                        + 4 * e * y * AstroMathUtils.sinDeg(m) * AstroMathUtils.cosDeg(2 * l0)
                        - 0.5 * y * y * AstroMathUtils.sinDeg(4 * l0)
                        - 1.25 * e * e * AstroMathUtils.sinDeg(2 * m)
        );

        // Solar Noon in UT hours
        double solarNoonUtHours = AstroMathUtils.normalizeHours((720.0 - 4.0 * longitude - eotMinutes) / 60.0);

        return new SolarParameters(declinationDeg, eotMinutes, solarNoonUtHours);
    }

    private static LocalTime utHoursToLocalTime(LocalDate date, double utHours, ZoneId zone) {
        if (Double.isNaN(utHours) || Double.isInfinite(utHours)) {
            return null;
        }
        double normalizedUt = AstroMathUtils.normalizeHours(utHours);
        int hours = (int) normalizedUt;
        double remMin = (normalizedUt - hours) * 60.0;
        int minutes = (int) remMin;
        int seconds = (int) Math.round((remMin - minutes) * 60.0);
        if (seconds >= 60) {
            seconds = 0;
            minutes++;
        }
        if (minutes >= 60) {
            minutes = 0;
            hours = (hours + 1) % 24;
        }

        ZonedDateTime utTime = ZonedDateTime.of(date, LocalTime.of(hours, minutes, seconds), ZoneId.of("UTC"));
        ZonedDateTime localZdt = utTime.withZoneSameInstant(zone);
        return localZdt.toLocalTime();
    }

    public record SolarParameters(
            double declinationDeg,
            double equationOfTimeMinutes,
            double solarNoonUtHours
    ) {
    }
}
