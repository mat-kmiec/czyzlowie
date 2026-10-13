package pl.matkmiec.backend.moon.dto;

import lombok.Getter;

@Getter
public enum MoonPhase {
    NEW_MOON("Nów", "New Moon", "wi-moon-alt-new", "moon-new", 0.0, 0.03),
    WAXING_CRESCENT("Przybywający sierp", "Waxing Crescent", "wi-moon-alt-waxing-crescent-3", "moon-waxing-crescent", 0.03, 0.22),
    FIRST_QUARTER("Pierwsza kwadra", "First Quarter", "wi-moon-alt-first-quarter", "moon-first-quarter", 0.22, 0.28),
    WAXING_GIBBOUS("Przybywający garb", "Waxing Gibbous", "wi-moon-alt-waxing-gibbous-3", "moon-waxing-gibbous", 0.28, 0.47),
    FULL_MOON("Pełnia", "Full Moon", "wi-moon-alt-full", "moon-full", 0.47, 0.53),
    WANING_GIBBOUS("Ubywający garb", "Waning Gibbous", "wi-moon-alt-waning-gibbous-3", "moon-waning-gibbous", 0.53, 0.72),
    LAST_QUARTER("Ostatnia kwadra", "Last Quarter", "wi-moon-alt-third-quarter", "moon-last-quarter", 0.72, 0.78),
    WANING_CRESCENT("Ubywający sierp", "Waning Crescent", "wi-moon-alt-waning-crescent-3", "moon-waning-crescent", 0.78, 0.97);

    private final String polishName;
    private final String englishName;
    private final String weatherIcon;
    private final String iconCode;
    private final double minPhase;
    private final double maxPhase;

    MoonPhase(String polishName, String englishName, String weatherIcon, String iconCode, double minPhase, double maxPhase) {
        this.polishName = polishName;
        this.englishName = englishName;
        this.weatherIcon = weatherIcon;
        this.iconCode = iconCode;
        this.minPhase = minPhase;
        this.maxPhase = maxPhase;
    }

    public static MoonPhase fromPhaseValue(double phase) {
        double normalized = phase - Math.floor(phase);
        if (normalized >= 0.97 || normalized < 0.03) {
            return NEW_MOON;
        } else if (normalized < 0.22) {
            return WAXING_CRESCENT;
        } else if (normalized < 0.28) {
            return FIRST_QUARTER;
        } else if (normalized < 0.47) {
            return WAXING_GIBBOUS;
        } else if (normalized < 0.53) {
            return FULL_MOON;
        } else if (normalized < 0.72) {
            return WANING_GIBBOUS;
        } else if (normalized < 0.78) {
            return LAST_QUARTER;
        } else {
            return WANING_CRESCENT;
        }
    }
}
