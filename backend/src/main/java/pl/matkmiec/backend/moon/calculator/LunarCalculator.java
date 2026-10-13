package pl.matkmiec.backend.moon.calculator;

import pl.matkmiec.backend.moon.dto.*;

import java.time.*;

public final class LunarCalculator {

    private static final double MOON_RADIUS_KM = 1737.4;
    private static final double PERIGEE_THRESHOLD_KM = 365000.0;
    private static final double APOGEE_THRESHOLD_KM = 404000.0;

    private LunarCalculator() {
    }

    public static GeneralMoonDataDto calculateGeneralMoonData(LocalDate date) {
        double jd = AstroMathUtils.toJulianDate(date, 12.0); // Noon UTC
        MoonPosition pos = calculateMoonPosition(jd);

        double phaseValue = pos.phaseValue();
        MoonPhase phaseEnum = MoonPhase.fromPhaseValue(phaseValue);
        MoonPhaseDto phaseDto = MoonPhaseDto.of(phaseEnum, phaseValue);

        double illuminationPercentage = Math.round(pos.illuminatedFraction() * 10000.0) / 100.0;
        double moonAgeDays = Math.round(phaseValue * AstroMathUtils.SYNODIC_MONTH * 100.0) / 100.0;
        double distanceKm = Math.round(pos.distanceKm() * 10.0) / 10.0;

        boolean isPerigee = distanceKm < PERIGEE_THRESHOLD_KM;
        boolean isApogee = distanceKm > APOGEE_THRESHOLD_KM;

        String distancePhenomenon;
        if (isPerigee) {
            distancePhenomenon = "Superksiężyc (Księżyc w perygeum - maksymalne zbliżenie do Ziemi, potężna grawitacja)";
        } else if (isApogee) {
            distancePhenomenon = "Mikroksiężyc (Księżyc w apogeum - maksymalne oddalenie od Ziemi)";
        } else {
            distancePhenomenon = "Średnia odległość orbitalna";
        }

        LocalDate nextNewMoon = findNextPhaseDate(date, 0.0);
        LocalDate nextFirstQuarter = findNextPhaseDate(date, 0.25);
        LocalDate nextFullMoon = findNextPhaseDate(date, 0.50);
        LocalDate nextLastQuarter = findNextPhaseDate(date, 0.75);

        return new GeneralMoonDataDto(
                phaseDto,
                illuminationPercentage,
                moonAgeDays,
                distanceKm,
                isPerigee,
                isApogee,
                distancePhenomenon,
                nextNewMoon,
                nextFirstQuarter,
                nextFullMoon,
                nextLastQuarter
        );
    }

    public static MoonTimesDto calculateMoonTimes(double latitude, double longitude, LocalDate date, ZoneId zoneId) {
        ZoneId zone = (zoneId != null) ? zoneId : ZoneId.of("Europe/Warsaw");

        // We compute 24 hourly points in UTC for the target date to find rise, set, transit, underfoot
        double[] altitudes = new double[25];
        double[] hourAngles = new double[25];
        double[] azimuths = new double[25];

        for (int h = 0; h <= 24; h++) {
            ZonedDateTime zdt = ZonedDateTime.of(date, LocalTime.MIN, zone).plusHours(h);
            ZonedDateTime utc = zdt.withZoneSameInstant(ZoneId.of("UTC"));
            double jd = AstroMathUtils.toJulianDate(utc);

            MoonPosition pos = calculateMoonPosition(jd);
            double gmst = AstroMathUtils.greenwichMeanSiderealTimeHours(jd);
            double lst = AstroMathUtils.normalizeHours(gmst + longitude / 15.0);
            double haHours = lst - pos.raHours();
            double haDeg = AstroMathUtils.normalizeDeg(haHours * 15.0);
            if (haDeg > 180.0) {
                haDeg -= 360.0;
            }

            double latRad = AstroMathUtils.rad(latitude);
            double decRad = AstroMathUtils.rad(pos.decDeg());
            double haRad = AstroMathUtils.rad(haDeg);

            double sinAlt = Math.sin(latRad) * Math.sin(decRad) + Math.cos(latRad) * Math.cos(decRad) * Math.cos(haRad);
            double altDeg = AstroMathUtils.asinDeg(sinAlt);

            // Horizontal coordinates parallax correction
            double parallax = AstroMathUtils.asinDeg(6378.14 / pos.distanceKm()) * Math.cos(AstroMathUtils.rad(altDeg));
            altDeg -= parallax;

            double cosAz = (Math.sin(decRad) - Math.sin(latRad) * Math.sin(AstroMathUtils.rad(altDeg)))
                    / (Math.cos(latRad) * Math.cos(AstroMathUtils.rad(altDeg)));
            double azDeg = AstroMathUtils.acosDeg(cosAz);
            if (Math.sin(haRad) > 0) {
                azDeg = 360.0 - azDeg;
            }

            altitudes[h] = altDeg;
            hourAngles[h] = haDeg;
            azimuths[h] = azDeg;
        }

        LocalTime moonrise = null;
        LocalTime moonset = null;
        LocalTime moonTransit = null;
        LocalTime moonUnderfoot = null;

        double targetAltitude = 0.125; // standard altitude for moon's upper limb at horizon

        for (int h = 0; h < 24; h++) {
            double alt1 = altitudes[h] - targetAltitude;
            double alt2 = altitudes[h + 1] - targetAltitude;

            // Moonrise: crossing from below to above horizon
            if (alt1 <= 0 && alt2 > 0 && moonrise == null) {
                double frac = -alt1 / (alt2 - alt1);
                double hour = h + frac;
                moonrise = hourToLocalTime(hour);
            }

            // Moonset: crossing from above to below horizon
            if (alt1 >= 0 && alt2 < 0 && moonset == null) {
                double frac = alt1 / (alt1 - alt2);
                double hour = h + frac;
                moonset = hourToLocalTime(hour);
            }

            // Upper culmination (Transit / Zenith): Hour angle crosses 0 from negative to positive
            double ha1 = hourAngles[h];
            double ha2 = hourAngles[h + 1];
            if (ha1 < 0 && ha2 >= 0 && moonTransit == null) {
                double frac = -ha1 / (ha2 - ha1);
                double hour = h + frac;
                moonTransit = hourToLocalTime(hour);
            }

            // Lower culmination (Underfoot / Nadir): Hour angle crosses ±180°
            if (ha1 > 90 && ha2 < -90 && moonUnderfoot == null) {
                double adjustedHa2 = ha2 + 360.0;
                double frac = (180.0 - ha1) / (adjustedHa2 - ha1);
                double hour = h + frac;
                moonUnderfoot = hourToLocalTime(hour);
            }
        }

        // Fallbacks if transits occurred close to day boundary
        if (moonTransit == null) {
            // Find hour of max altitude
            int maxIdx = 0;
            for (int i = 1; i <= 24; i++) {
                if (altitudes[i] > altitudes[maxIdx]) {
                    maxIdx = i;
                }
            }
            moonTransit = hourToLocalTime(maxIdx);
        }

        if (moonUnderfoot == null) {
            // Underfoot is approx 12h 25m apart from transit
            if (moonTransit != null) {
                moonUnderfoot = moonTransit.plusHours(12).plusMinutes(25);
            }
        }

        // Midday altitude and azimuth
        Double middayAltitude = Math.round(altitudes[12] * 10.0) / 10.0;
        Double middayAzimuth = Math.round(azimuths[12] * 10.0) / 10.0;

        return new MoonTimesDto(
                moonrise,
                moonset,
                moonTransit,
                moonUnderfoot,
                middayAltitude,
                middayAzimuth
        );
    }

    public static MoonPosition calculateMoonPosition(double jd) {
        double t = AstroMathUtils.toJulianCenturies(jd);

        // Moon's mean longitude
        double lPrime = AstroMathUtils.normalizeDeg(218.3164477 + 481267.88123421 * t - 0.0015786 * t * t);

        // Moon's mean elongation
        double d = AstroMathUtils.normalizeDeg(297.8501921 + 445267.1114034 * t - 0.0018819 * t * t);

        // Sun's mean anomaly
        double m = AstroMathUtils.normalizeDeg(357.5291092 + 35999.0502909 * t - 0.0001536 * t * t);

        // Moon's mean anomaly
        double mPrime = AstroMathUtils.normalizeDeg(134.9633964 + 477198.8675055 * t + 0.0087414 * t * t);

        // Moon's argument of latitude
        double f = AstroMathUtils.normalizeDeg(93.2720950 + 483202.0175233 * t - 0.0036539 * t * t);

        // Periodic perturbations in longitude (arcseconds -> degrees)
        double sigmaL = 22640.0 * AstroMathUtils.sinDeg(mPrime)
                - 4586.0 * AstroMathUtils.sinDeg(mPrime - 2.0 * d)
                + 2370.0 * AstroMathUtils.sinDeg(2.0 * d)
                + 769.0 * AstroMathUtils.sinDeg(2.0 * mPrime)
                - 668.0 * AstroMathUtils.sinDeg(m)
                - 412.0 * AstroMathUtils.sinDeg(2.0 * f)
                - 212.0 * AstroMathUtils.sinDeg(2.0 * mPrime - 2.0 * d)
                - 206.0 * AstroMathUtils.sinDeg(mPrime + m - 2.0 * d)
                + 192.0 * AstroMathUtils.sinDeg(mPrime + 2.0 * d)
                - 165.0 * AstroMathUtils.sinDeg(m - 2.0 * d)
                - 125.0 * AstroMathUtils.sinDeg(d)
                - 110.0 * AstroMathUtils.sinDeg(mPrime + m)
                + 148.0 * AstroMathUtils.sinDeg(mPrime - m);

        double lambda = AstroMathUtils.normalizeDeg(lPrime + sigmaL / 3600.0);

        // Periodic perturbations in latitude (arcseconds -> degrees)
        double sigmaB = 18461.0 * AstroMathUtils.sinDeg(f)
                - 1010.0 * AstroMathUtils.sinDeg(mPrime - f)
                + 1010.0 * AstroMathUtils.sinDeg(mPrime + f)
                - 439.0 * AstroMathUtils.sinDeg(f - 2.0 * d)
                - 223.0 * AstroMathUtils.sinDeg(mPrime - 2.0 * d - f)
                + 217.0 * AstroMathUtils.sinDeg(mPrime - 2.0 * d + f);

        double beta = sigmaB / 3600.0;

        // Periodic perturbations in distance (meters -> km)
        double sigmaR = -20954000.0 * AstroMathUtils.cosDeg(mPrime)
                - 3699000.0 * AstroMathUtils.cosDeg(2.0 * d - mPrime)
                - 2956000.0 * AstroMathUtils.cosDeg(2.0 * d)
                - 570000.0 * AstroMathUtils.cosDeg(2.0 * mPrime)
                + 246000.0 * AstroMathUtils.cosDeg(2.0 * mPrime - 2.0 * d)
                - 205000.0 * AstroMathUtils.cosDeg(m - 2.0 * d)
                - 171000.0 * AstroMathUtils.cosDeg(mPrime + m - 2.0 * d)
                - 152000.0 * AstroMathUtils.cosDeg(mPrime + 2.0 * d);

        double distanceKm = 385000.56 + sigmaR / 1000.0;

        // Obliquity of ecliptic
        double eps = 23.439291 - 0.0130042 * t;

        // Equatorial coordinates (Right Ascension & Declination)
        double cosBeta = AstroMathUtils.cosDeg(beta);
        double sinBeta = AstroMathUtils.sinDeg(beta);
        double sinLambda = AstroMathUtils.sinDeg(lambda);
        double cosLambda = AstroMathUtils.cosDeg(lambda);
        double sinEps = AstroMathUtils.sinDeg(eps);
        double cosEps = AstroMathUtils.cosDeg(eps);

        double raDeg = AstroMathUtils.atan2Deg(
                sinLambda * cosEps - (sinBeta / cosBeta) * sinEps,
                cosLambda
        );
        raDeg = AstroMathUtils.normalizeDeg(raDeg);
        double raHours = raDeg / 15.0;

        double decDeg = AstroMathUtils.asinDeg(
                sinBeta * cosEps + cosBeta * sinEps * sinLambda
        );

        // Phase angle & illuminated fraction
        double phaseAngleDeg = 180.0 - d;
        double illuminatedFraction = (1.0 + AstroMathUtils.cosDeg(phaseAngleDeg)) / 2.0;
        double phaseValue = AstroMathUtils.normalizeDeg(d) / 360.0;

        return new MoonPosition(raHours, decDeg, distanceKm, phaseValue, illuminatedFraction);
    }

    private static LocalDate findNextPhaseDate(LocalDate startDate, double targetPhase) {
        double currentPhase = calculateMoonPosition(AstroMathUtils.toJulianDate(startDate, 12.0)).phaseValue();

        double diff = (targetPhase >= currentPhase) ? (targetPhase - currentPhase) : (1.0 + targetPhase - currentPhase);
        int approxDays = (int) Math.floor(diff * AstroMathUtils.SYNODIC_MONTH);
        int startSearch = Math.max(1, approxDays - 2);

        LocalDate prevDay = startDate.plusDays(startSearch - 1);
        double prevPhase = calculateMoonPosition(AstroMathUtils.toJulianDate(prevDay, 12.0)).phaseValue();

        int maxSearch = Math.min(36, approxDays + 4);
        for (int i = startSearch; i <= maxSearch; i++) {
            LocalDate nextDay = startDate.plusDays(i);
            double nextPhase = calculateMoonPosition(AstroMathUtils.toJulianDate(nextDay, 12.0)).phaseValue();

            if (targetPhase == 0.0) {
                if (prevPhase > 0.85 && nextPhase < 0.15) {
                    return nextDay;
                }
            } else {
                if (prevPhase < targetPhase && nextPhase >= targetPhase) {
                    return nextDay;
                }
            }
            prevPhase = nextPhase;
        }

        // Fallback jeśli faza wypadła poza oknem przybliżenia
        prevPhase = currentPhase;
        for (int i = 1; i <= 35; i++) {
            LocalDate nextDay = startDate.plusDays(i);
            double nextPhase = calculateMoonPosition(AstroMathUtils.toJulianDate(nextDay, 12.0)).phaseValue();
            if (targetPhase == 0.0) {
                if (prevPhase > 0.85 && nextPhase < 0.15) {
                    return nextDay;
                }
            } else {
                if (prevPhase < targetPhase && nextPhase >= targetPhase) {
                    return nextDay;
                }
            }
            prevPhase = nextPhase;
        }

        return startDate.plusDays((long) Math.round(diff * AstroMathUtils.SYNODIC_MONTH));
    }

    private static LocalTime hourToLocalTime(double decimalHour) {
        double normalized = AstroMathUtils.normalizeHours(decimalHour);
        int h = (int) normalized;
        double remMin = (normalized - h) * 60.0;
        int m = (int) remMin;
        int s = (int) Math.round((remMin - m) * 60.0);
        if (s >= 60) {
            s = 0;
            m++;
        }
        if (m >= 60) {
            m = 0;
            h = (h + 1) % 24;
        }
        return LocalTime.of(h, m, s);
    }

    public record MoonPosition(
            double raHours,
            double decDeg,
            double distanceKm,
            double phaseValue,
            double illuminatedFraction
    ) {
    }
}
