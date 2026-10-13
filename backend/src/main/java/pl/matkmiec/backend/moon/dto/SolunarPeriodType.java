package pl.matkmiec.backend.moon.dto;

import lombok.Getter;

@Getter
public enum SolunarPeriodType {
    MAJOR("Główny okres żerowania", "Major Period", 120),
    MINOR("Pomniejszy okres żerowania", "Minor Period", 60);

    private final String polishName;
    private final String englishName;
    private final int defaultDurationMinutes;

    SolunarPeriodType(String polishName, String englishName, int defaultDurationMinutes) {
        this.polishName = polishName;
        this.englishName = englishName;
        this.defaultDurationMinutes = defaultDurationMinutes;
    }
}
