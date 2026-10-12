package pl.matkmiec.backend.airquality.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record AirQualityOverviewDto(
        long totalStationsCount,
        long activeStationsCount,
        Map<String, Long> stationsByIndexLevel,
        List<AirQualityRankingDto> topCleanest,
        List<AirQualityRankingDto> topMostPolluted,
        LocalDateTime lastUpdated
) {}
