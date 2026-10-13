package pl.matkmiec.backend.moon.dto;

public record MoonPhaseDto(
        MoonPhase phase,
        String polishName,
        String englishName,
        String weatherIcon,
        String iconCode,
        double phaseValue
) {
    public static MoonPhaseDto of(MoonPhase phase, double phaseValue) {
        return new MoonPhaseDto(
                phase,
                phase.getPolishName(),
                phase.getEnglishName(),
                phase.getWeatherIcon(),
                phase.getIconCode(),
                Math.round(phaseValue * 10000.0) / 10000.0
        );
    }
}
