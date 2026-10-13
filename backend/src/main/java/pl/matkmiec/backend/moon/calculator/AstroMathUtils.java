package pl.matkmiec.backend.moon.calculator;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class AstroMathUtils {

    public static final double PI = Math.PI;
    public static final double TWO_PI = 2.0 * Math.PI;
    public static final double RAD = Math.PI / 180.0;
    public static final double DEG = 180.0 / Math.PI;
    public static final double SYNODIC_MONTH = 29.530588853;

    private AstroMathUtils() {
    }

    public static double rad(double deg) {
        return deg * RAD;
    }

    public static double deg(double rad) {
        return rad * DEG;
    }

    public static double sinDeg(double deg) {
        return Math.sin(deg * RAD);
    }

    public static double cosDeg(double deg) {
        return Math.cos(deg * RAD);
    }

    public static double tanDeg(double deg) {
        return Math.tan(deg * RAD);
    }

    public static double asinDeg(double val) {
        return Math.asin(Math.max(-1.0, Math.min(1.0, val))) * DEG;
    }

    public static double acosDeg(double val) {
        return Math.acos(Math.max(-1.0, Math.min(1.0, val))) * DEG;
    }

    public static double atan2Deg(double y, double x) {
        return Math.atan2(y, x) * DEG;
    }

    public static double normalizeDeg(double deg) {
        double result = deg % 360.0;
        if (result < 0) {
            result += 360.0;
        }
        return result;
    }

    public static double normalizeHours(double hours) {
        double result = hours % 24.0;
        if (result < 0) {
            result += 24.0;
        }
        return result;
    }

    public static double toJulianDate(LocalDate date, double utHour) {
        int year = date.getYear();
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        if (month <= 2) {
            year -= 1;
            month += 12;
        }

        int a = year / 100;
        int b = 2 - a + (a / 4);

        return Math.floor(365.25 * (year + 4716))
                + Math.floor(30.6001 * (month + 1))
                + day + b - 1524.5
                + (utHour / 24.0);
    }

    public static double toJulianDate(ZonedDateTime zdt) {
        ZonedDateTime utc = zdt.withZoneSameInstant(ZoneId.of("UTC"));
        double utHour = utc.getHour() + utc.getMinute() / 60.0 + utc.getSecond() / 3600.0 + utc.getNano() / 3.6e12;
        return toJulianDate(utc.toLocalDate(), utHour);
    }

    public static double toJulianCenturies(double julianDate) {
        return (julianDate - 2451545.0) / 36525.0;
    }

    public static double greenwichMeanSiderealTimeHours(double jd) {
        double t = (jd - 2451545.0) / 36525.0;
        double gmst = 280.46061837 + 360.98564736629 * (jd - 2451545.0)
                + 0.000387933 * t * t - (t * t * t) / 38710000.0;
        return normalizeHours(normalizeDeg(gmst) / 15.0);
    }

    public static LocalTime decimalHoursToLocalTime(double decimalHours) {
        if (Double.isNaN(decimalHours) || Double.isInfinite(decimalHours)) {
            return null;
        }
        double normalized = normalizeHours(decimalHours);
        int hours = (int) normalized;
        double remainingMinutes = (normalized - hours) * 60.0;
        int minutes = (int) remainingMinutes;
        int seconds = (int) Math.round((remainingMinutes - minutes) * 60.0);
        if (seconds >= 60) {
            seconds = 0;
            minutes++;
        }
        if (minutes >= 60) {
            minutes = 0;
            hours = (hours + 1) % 24;
        }
        return LocalTime.of(hours, minutes, seconds);
    }
}
