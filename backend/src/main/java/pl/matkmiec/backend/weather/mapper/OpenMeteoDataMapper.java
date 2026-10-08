package pl.matkmiec.backend.weather.mapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pl.matkmiec.backend.weather.dto.OpenMeteoResponseDto;
import pl.matkmiec.backend.weather.model.ImgwSynopStation;
import pl.matkmiec.backend.weather.model.OpenMeteoForecast;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class OpenMeteoDataMapper {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public List<OpenMeteoForecast> toForecastEntities(OpenMeteoResponseDto dto, ImgwSynopStation station, LocalDateTime generatedAt) {
        if (dto == null || dto.hourly() == null || dto.hourly().time() == null || station == null) {
            return Collections.emptyList();
        }

        OpenMeteoResponseDto.HourlyDataDto hourly = dto.hourly();
        List<String> times = hourly.time();
        int size = times.size();

        LocalDateTime effectiveGeneratedAt = generatedAt != null ? generatedAt : LocalDateTime.now();
        List<OpenMeteoForecast> forecasts = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            LocalDateTime forecastTime = parseForecastTime(times.get(i));
            if (forecastTime == null) {
                continue;
            }

            Double temp = getOrNull(hourly.temperature2m(), i);
            Double humidity = getOrNull(hourly.relativeHumidity2m(), i);
            Double precip = getOrNull(hourly.precipitation(), i);
            Double pressure = resolvePressure(hourly, i);
            Double windSpeed = getOrNull(hourly.windSpeed10m(), i);
            Integer windDir = getOrNull(hourly.windDirection10m(), i);

            OpenMeteoForecast forecast = OpenMeteoForecast.builder()
                    .synopStation(station)
                    .forecastTime(forecastTime)
                    .generatedAt(effectiveGeneratedAt)
                    .temperature(temp)
                    .relativeHumidity(humidity)
                    .precipitation(precip)
                    .pressure(pressure)
                    .windSpeed(windSpeed)
                    .windDirection(windDir)
                    .build();

            forecasts.add(forecast);
        }

        return forecasts;
    }

    public LocalDateTime parseForecastTime(String timeStr) {
        if (timeStr == null || timeStr.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(timeStr.trim(), ISO_FORMATTER);
        } catch (Exception e) {
            log.debug("Nie udało się sparsować czasu prognozy Open-Meteo: {}", timeStr);
            return null;
        }
    }

    private Double resolvePressure(OpenMeteoResponseDto.HourlyDataDto hourly, int index) {
        Double msl = getOrNull(hourly.pressureMsl(), index);
        if (msl != null) {
            return msl;
        }
        return getOrNull(hourly.surfacePressure(), index);
    }

    private <T> T getOrNull(List<T> list, int index) {
        if (list == null || index < 0 || index >= list.size()) {
            return null;
        }
        return list.get(index);
    }
}
