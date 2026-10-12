package pl.matkmiec.backend.airquality.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record AirQualityMapPointDto(
        Long stationId,
        String stationName,
        Double lat,
        Double lon,
        String addressStreet,
        String cityName,
        String districtName,
        String provinceName,
        String terytCode,
        Integer overallIndexLevelId,
        String overallIndexLevelName,
        String criticalParam,
        Map<String, PollutantMeasurementDto> pollutants,
        LocalDateTime lastUpdated
) {}
