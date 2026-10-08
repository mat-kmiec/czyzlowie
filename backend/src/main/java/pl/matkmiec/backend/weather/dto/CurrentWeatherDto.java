package pl.matkmiec.backend.weather.dto;

import lombok.Builder;
import lombok.Value;
import pl.matkmiec.backend.weather.model.ImgwHydroData;
import pl.matkmiec.backend.weather.model.ImgwMeteoData;
import pl.matkmiec.backend.weather.model.ImgwSynopData;
import pl.matkmiec.backend.weather.model.OpenMeteoForecast;

import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
public class CurrentWeatherDto {
    String stationId;
    String stationName;
    Double lat;
    Double lon;
    Double distanceKm;
    LocalDateTime measurementTime;
    Double temperature;
    Double windSpeed;
    Integer windDirection;
    Double relativeHumidity;
    Double totalPrecipitation;
    Double pressure;
    ImgwSynopData synopData;
    ImgwMeteoData meteoData;
    ImgwHydroData hydroData;
    List<OpenMeteoForecast> upcomingForecast;
}
