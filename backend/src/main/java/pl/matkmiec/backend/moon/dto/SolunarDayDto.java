package pl.matkmiec.backend.moon.dto;

import java.util.List;

public record SolunarDayDto(
        SolunarActivityLevel overallRating,
        int overallScore,
        int stars,
        String summary,
        List<SolunarPeriodDto> majorPeriods,
        List<SolunarPeriodDto> minorPeriods,
        List<SolunarPeriodDto> allPeriods,
        List<String> peakFishingWindows,
        String moonPhaseInfluence,
        String fishingTip
) {
}
