package pl.matkmiec.backend.airquality.mapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pl.matkmiec.backend.airquality.model.IosAirQualityForecast;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class IosForecastMapper {

    private static final DateTimeFormatter BASIC_ISO_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter ISO_LOCAL_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public List<IosAirQualityForecast> toForecastEntities(String terytCode, String pollutant, Map<String, Double> forecastMap) {
        if (forecastMap == null || forecastMap.isEmpty() || terytCode == null || pollutant == null) {
            return List.of();
        }

        List<IosAirQualityForecast> entities = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (Map.Entry<String, Double> entry : forecastMap.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                LocalDate date = parseDate(entry.getKey());
                if (date != null) {
                    entities.add(IosAirQualityForecast.builder()
                            .terytCode(terytCode.trim())
                            .pollutant(pollutant.trim().toUpperCase())
                            .forecastDate(date)
                            .forecastValue(entry.getValue())
                            .updatedAt(now)
                            .build());
                }
            }
        }

        return entities;
    }

    public LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        String clean = dateStr.trim();
        try {
            if (clean.length() == 8 && clean.chars().allMatch(Character::isDigit)) {
                return LocalDate.parse(clean, BASIC_ISO_DATE);
            } else if (clean.contains("-")) {
                return LocalDate.parse(clean, ISO_LOCAL_DATE);
            } else {
                return LocalDate.parse(clean, BASIC_ISO_DATE);
            }
        } catch (DateTimeParseException e) {
            log.warn("Nie udało się sparsować daty prognozy IOŚ '{}': {}", dateStr, e.getMessage());
            return null;
        }
    }
}
