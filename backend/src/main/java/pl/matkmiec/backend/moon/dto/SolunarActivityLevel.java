package pl.matkmiec.backend.moon.dto;

import lombok.Getter;

@Getter
public enum SolunarActivityLevel {
    POOR("Słaba", "Poor", 1, "Niska aktywność żerowania ryb"),
    FAIR("Przeciętna", "Fair", 2, "Umiarkowana aktywność żerowania"),
    GOOD("Dobra", "Good", 3, "Dobre warunki na udany połów"),
    VERY_GOOD("Bardzo dobra", "Very Good", 4, "Wysoka aktywność ryb, optymalne godziny żerowania"),
    EXCELLENT("Doskonała", "Excellent", 5, "Wybitne warunki wędkarskie, szczyt żerowania");

    private final String polishName;
    private final String englishName;
    private final int stars;
    private final String description;

    SolunarActivityLevel(String polishName, String englishName, int stars, String description) {
        this.polishName = polishName;
        this.englishName = englishName;
        this.stars = stars;
        this.description = description;
    }

    public static SolunarActivityLevel fromScore(int score) {
        if (score >= 85) {
            return EXCELLENT;
        } else if (score >= 70) {
            return VERY_GOOD;
        } else if (score >= 50) {
            return GOOD;
        } else if (score >= 30) {
            return FAIR;
        } else {
            return POOR;
        }
    }
}
