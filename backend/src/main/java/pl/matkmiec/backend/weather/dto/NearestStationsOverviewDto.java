package pl.matkmiec.backend.weather.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class NearestStationsOverviewDto {
    NearestStationDto nearestSynop;
    NearestStationDto nearestMeteo;
    NearestStationDto nearestHydro;
}
