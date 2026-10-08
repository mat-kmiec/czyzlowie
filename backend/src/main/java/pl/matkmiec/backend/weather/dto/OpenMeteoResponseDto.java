package pl.matkmiec.backend.weather.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record OpenMeteoResponseDto(
        @JsonProperty("latitude") Double latitude,
        @JsonProperty("longitude") Double longitude,
        @JsonProperty("generationtime_ms") Double generationTimeMs,
        @JsonProperty("utc_offset_seconds") Integer utcOffsetSeconds,
        @JsonProperty("timezone") String timezone,
        @JsonProperty("timezone_abbreviation") String timezoneAbbreviation,
        @JsonProperty("elevation") Double elevation,
        @JsonProperty("hourly_units") HourlyUnitsDto hourlyUnits,
        @JsonProperty("hourly") HourlyDataDto hourly
) {
    public record HourlyUnitsDto(
            @JsonProperty("time") String time,
            @JsonProperty("temperature_2m") String temperature2m,
            @JsonProperty("relative_humidity_2m") String relativeHumidity2m,
            @JsonProperty("precipitation") String precipitation,
            @JsonProperty("surface_pressure") String surfacePressure,
            @JsonProperty("pressure_msl") String pressureMsl,
            @JsonProperty("wind_speed_10m") String windSpeed10m,
            @JsonProperty("wind_direction_10m") String windDirection10m
    ) {}

    public record HourlyDataDto(
            @JsonProperty("time") List<String> time,
            @JsonProperty("temperature_2m") List<Double> temperature2m,
            @JsonProperty("relative_humidity_2m") List<Double> relativeHumidity2m,
            @JsonProperty("precipitation") List<Double> precipitation,
            @JsonProperty("surface_pressure") List<Double> surfacePressure,
            @JsonProperty("pressure_msl") List<Double> pressureMsl,
            @JsonProperty("wind_speed_10m") List<Double> windSpeed10m,
            @JsonProperty("wind_direction_10m") List<Integer> windDirection10m
    ) {}
}
