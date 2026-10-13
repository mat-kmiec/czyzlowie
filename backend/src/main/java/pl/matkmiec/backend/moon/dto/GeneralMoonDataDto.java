package pl.matkmiec.backend.moon.dto;

import java.time.LocalDate;

public record GeneralMoonDataDto(
        MoonPhaseDto phase,
        double illuminationPercentage,
        double moonAgeDays,
        double distanceKm,
        boolean isPerigee,
        boolean isApogee,
        String distancePhenomenon,
        LocalDate nextNewMoon,
        LocalDate nextFirstQuarter,
        LocalDate nextFullMoon,
        LocalDate nextLastQuarter
) {
}
