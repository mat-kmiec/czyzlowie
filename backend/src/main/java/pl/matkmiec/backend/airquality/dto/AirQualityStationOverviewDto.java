package pl.matkmiec.backend.airquality.dto;

import pl.matkmiec.backend.airquality.model.GiosAirQualityIndex;
import pl.matkmiec.backend.airquality.model.GiosSensor;
import pl.matkmiec.backend.airquality.model.GiosStation;

import java.util.List;

public record AirQualityStationOverviewDto(
        GiosStation station,
        List<GiosSensor> sensors,
        GiosAirQualityIndex airQualityIndex
) {}
