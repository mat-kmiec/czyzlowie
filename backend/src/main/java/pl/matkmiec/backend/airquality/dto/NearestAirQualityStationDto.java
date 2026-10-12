package pl.matkmiec.backend.airquality.dto;

import pl.matkmiec.backend.airquality.model.GiosStation;

import java.util.Map;

public record NearestAirQualityStationDto(
        GiosStation station,
        Double distanceKm,
        Integer overallIndexLevelId,
        String overallIndexLevelName,
        String criticalParam,
        Map<String, PollutantMeasurementDto> pollutants
) {}
