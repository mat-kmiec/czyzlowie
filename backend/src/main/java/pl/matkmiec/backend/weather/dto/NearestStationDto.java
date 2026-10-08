package pl.matkmiec.backend.weather.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class NearestStationDto {
    String id;
    String name;
    String stationType;
    Double lat;
    Double lon;
    Double distanceKm;
}
