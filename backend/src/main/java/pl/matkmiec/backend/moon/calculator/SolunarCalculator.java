package pl.matkmiec.backend.moon.calculator;

import pl.matkmiec.backend.moon.dto.*;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class SolunarCalculator {

    private SolunarCalculator() {
    }

    public static SolunarDayDto calculateSolunarDay(GeneralMoonDataDto generalMoon, SunTimesDto sunTimes, MoonTimesDto moonTimes) {
        List<SolunarPeriodDto> majorPeriods = new ArrayList<>();
        List<SolunarPeriodDto> minorPeriods = new ArrayList<>();
        List<String> peakWindows = new ArrayList<>();

        // Base score from moon phase
        double phase = generalMoon.phase().phaseValue();
        // Distance to nearest full moon (0.5) or new moon (0.0 / 1.0)
        double distToNew = Math.min(phase, 1.0 - phase);
        double distToFull = Math.abs(phase - 0.5);
        double minDistToPeak = Math.min(distToNew, distToFull); // 0.0 at new/full, 0.25 at quarters

        // Score: 95 at peak (new/full), 55 at quarter
        int baseScore = (int) Math.round(95.0 - (minDistToPeak / 0.25) * 40.0);

        // Adjust for perigee / apogee
        if (generalMoon.isPerigee()) {
            baseScore += 10;
        } else if (generalMoon.isApogee()) {
            baseScore -= 5;
        }

        // 1. Major Period 1: Moon Transit (Zenith)
        if (moonTimes.moonTransit() != null) {
            LocalTime start = moonTimes.moonTransit().minusMinutes(60);
            LocalTime end = moonTimes.moonTransit().plusMinutes(60);
            int rating = Math.min(100, baseScore + 5);
            majorPeriods.add(new SolunarPeriodDto(
                    SolunarPeriodType.MAJOR,
                    start,
                    end,
                    "Główny okres 1 (Księżyc w zenicie)",
                    "Maksymalna siła grawitacji Księżyca nad łowiskiem - intensywne żerowanie ryb drapieżnych i spokojnego żeru",
                    rating
            ));
            checkSolarOverlap(start, end, sunTimes, "Zenit Księżyca", peakWindows);
        }

        // 2. Major Period 2: Moon Underfoot (Nadir)
        if (moonTimes.moonUnderfoot() != null) {
            LocalTime start = moonTimes.moonUnderfoot().minusMinutes(60);
            LocalTime end = moonTimes.moonUnderfoot().plusMinutes(60);
            int rating = Math.min(100, baseScore);
            majorPeriods.add(new SolunarPeriodDto(
                    SolunarPeriodType.MAJOR,
                    start,
                    end,
                    "Główny okres 2 (Księżyc w nadirze)",
                    "Księżyc po przeciwnej stronie Ziemi - drugi główny impuls do żerowania w ciągu doby",
                    rating
            ));
            checkSolarOverlap(start, end, sunTimes, "Nadir Księżyca", peakWindows);
        }

        // 3. Minor Period 1: Moonrise
        if (moonTimes.moonrise() != null) {
            LocalTime start = moonTimes.moonrise().minusMinutes(30);
            LocalTime end = moonTimes.moonrise().plusMinutes(30);
            int rating = Math.min(100, Math.max(10, baseScore - 10));
            minorPeriods.add(new SolunarPeriodDto(
                    SolunarPeriodType.MINOR,
                    start,
                    end,
                    "Pomniejszy okres 1 (Wschód Księżyca)",
                    "Pojawienie się tarczy Księżyca nad horyzontem wywołuje krótką falę aktywności ryb",
                    rating
            ));
            checkSolarOverlap(start, end, sunTimes, "Wschód Księżyca", peakWindows);
        }

        // 4. Minor Period 2: Moonset
        if (moonTimes.moonset() != null) {
            LocalTime start = moonTimes.moonset().minusMinutes(30);
            LocalTime end = moonTimes.moonset().plusMinutes(30);
            int rating = Math.min(100, Math.max(10, baseScore - 10));
            minorPeriods.add(new SolunarPeriodDto(
                    SolunarPeriodType.MINOR,
                    start,
                    end,
                    "Pomniejszy okres 2 (Zachód Księżyca)",
                    "Zejście Księżyca pod horyzont - około godzinny wzrost brań",
                    rating
            ));
            checkSolarOverlap(start, end, sunTimes, "Zachód Księżyca", peakWindows);
        }

        // If there were overlaps, bonus score
        if (!peakWindows.isEmpty()) {
            baseScore += (peakWindows.size() * 5);
        }

        int finalScore = Math.min(100, Math.max(15, baseScore));
        SolunarActivityLevel rating = SolunarActivityLevel.fromScore(finalScore);

        List<SolunarPeriodDto> allPeriods = new ArrayList<>();
        allPeriods.addAll(majorPeriods);
        allPeriods.addAll(minorPeriods);
        allPeriods.sort(Comparator.comparing(SolunarPeriodDto::startTime));

        String summary = buildSummary(rating, generalMoon, peakWindows);
        String moonPhaseInfluence = buildPhaseInfluence(generalMoon);
        String fishingTip = buildFishingTip(rating, generalMoon, peakWindows);

        return new SolunarDayDto(
                rating,
                finalScore,
                rating.getStars(),
                summary,
                majorPeriods,
                minorPeriods,
                allPeriods,
                peakWindows,
                moonPhaseInfluence,
                fishingTip
        );
    }

    private static void checkSolarOverlap(LocalTime pStart, LocalTime pEnd, SunTimesDto sun, String lunarEventName, List<String> peakWindows) {
        if (sun == null) return;

        // Check sunrise overlap (±45 min)
        if (sun.sunrise() != null && isOverlap(pStart, pEnd, sun.sunrise().minusMinutes(45), sun.sunrise().plusMinutes(45))) {
            peakWindows.add(String.format("%s - %s (%s pokrywa się ze wschodem Słońca - Złote Okno Brań)",
                    formatTime(pStart), formatTime(pEnd), lunarEventName));
        }

        // Check sunset overlap (±45 min)
        if (sun.sunset() != null && isOverlap(pStart, pEnd, sun.sunset().minusMinutes(45), sun.sunset().plusMinutes(45))) {
            peakWindows.add(String.format("%s - %s (%s pokrywa się z zachodem Słońca - Szczyt aktywności drapieżników)",
                    formatTime(pStart), formatTime(pEnd), lunarEventName));
        }
    }

    private static boolean isOverlap(LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
        if (start1.isAfter(end1) || start2.isAfter(end2)) {
            // crosses midnight
            return true;
        }
        return !start1.isAfter(end2) && !end1.isBefore(start2);
    }

    private static String formatTime(LocalTime time) {
        return String.format("%02d:%02d", time.getHour(), time.getMinute());
    }

    private static String buildSummary(SolunarActivityLevel rating, GeneralMoonDataDto generalMoon, List<String> peakWindows) {
        StringBuilder sb = new StringBuilder();
        sb.append(rating.getDescription()).append(". ");
        sb.append("Faza: ").append(generalMoon.phase().polishName()).append(" (")
                .append(generalMoon.illuminationPercentage()).append("% oświetlenia). ");

        if (generalMoon.isPerigee()) {
            sb.append("Wzmocnienie perygeum: Księżyc znajduje się bardzo blisko Ziemi (").append(generalMoon.distanceKm()).append(" km), co silnie stymuluje ryby. ");
        }

        if (!peakWindows.isEmpty()) {
            sb.append("Występują nakładające się okna solunarne i słoneczne, gwarantujące najwyższą koncentrację brań!");
        } else {
            sb.append("Skup się na głównych okresach żerowania (Major Periods).");
        }
        return sb.toString();
    }

    private static String buildPhaseInfluence(GeneralMoonDataDto generalMoon) {
        MoonPhase phase = generalMoon.phase().phase();
        return switch (phase) {
            case NEW_MOON -> "Nów: Najsilniejszy impuls grawitacyjny w cyklu księżycowym. Bardzo wysoka aktywność żerowa ryb.";
            case FULL_MOON -> "Pełnia: Maksymalne oświetlenie nocne i wysokie ciśnienie pływowe. Ryby żerują intensywnie o zmierzchu, w nocy i nad ranem.";
            case FIRST_QUARTER, LAST_QUARTER -> "Kwadra: Umiarkowana siła grawitacyjna. Brania mogą być bardziej punktowe i skupione wyłącznie wokół tranzytów.";
            case WAXING_CRESCENT, WAXING_GIBBOUS -> "Księżyc przybywający: Dobry czas na połów, tendencja wzrostowa w aktywności drapieżników.";
            case WANING_GIBBOUS, WANING_CRESCENT -> "Księżyc ubywający: Stabilne warunki żerowe, najlepsze wyniki w okolicach świtu i zmierzchu.";
        };
    }

    private static String buildFishingTip(SolunarActivityLevel rating, GeneralMoonDataDto generalMoon, List<String> peakWindows) {
        if (!peakWindows.isEmpty()) {
            return "Zaplanuj zasiadkę / spining dokładnie na czas podany w Złotych Oknach Brań. Wtedy ryby pobierają pokarm z największą intensywnością.";
        }
        if (rating == SolunarActivityLevel.EXCELLENT || rating == SolunarActivityLevel.VERY_GOOD) {
            return "Znakomity dzień na wyprawę wędkarską. Wykorzystaj 2-godzinne okresy Major wokół zenitu i nadiru Księżyca.";
        }
        return "Podczas słabszych dni kluczowe jest precyzyjne nęcenie i celowanie w dokładnie wyznaczone okresy Major i Minor.";
    }
}
