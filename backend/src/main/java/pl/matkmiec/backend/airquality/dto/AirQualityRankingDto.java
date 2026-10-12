package pl.matkmiec.backend.airquality.dto;

import java.time.LocalDateTime;

public record AirQualityRankingDto(
        int rank,
        Long stationId,
        String stationName,
        String cityName,
        String districtName,
        String provinceName,
        Double lat,
        Double lon,
        String paramCode,
        Double value,
        String unit,
        Integer indexLevelId,
        String indexLevelName,
        LocalDateTime measurementTime
) {}
