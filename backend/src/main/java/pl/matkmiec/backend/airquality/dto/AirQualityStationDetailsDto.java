package pl.matkmiec.backend.airquality.dto;

import pl.matkmiec.backend.airquality.model.GiosAirQualityIndex;
import pl.matkmiec.backend.airquality.model.GiosSensor;
import pl.matkmiec.backend.airquality.model.GiosStation;

import java.util.List;
import java.util.Map;

public record AirQualityStationDetailsDto(
        GiosStation station,
        GiosAirQualityIndex airQualityIndex,
        Map<String, PollutantMeasurementDto> currentMeasurements,
        Map<String, List<HistoricalDataPointDto>> historical24h,
        List<GiosSensor> availableSensors,
        List<AirQualityForecastDto> forecasts
) {}
