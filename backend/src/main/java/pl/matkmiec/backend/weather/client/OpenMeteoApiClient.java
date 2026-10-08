package pl.matkmiec.backend.weather.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import pl.matkmiec.backend.weather.dto.OpenMeteoResponseDto;
import pl.matkmiec.backend.weather.exception.WeatherApiClientException;

@Slf4j
@Component
public class OpenMeteoApiClient {

    private final RestClient openMeteoRestClient;

    public OpenMeteoApiClient(@Qualifier("openMeteoRestClient") RestClient openMeteoRestClient) {
        this.openMeteoRestClient = openMeteoRestClient;
    }

    @Retry(name = "default")
    @CircuitBreaker(name = "default", fallbackMethod = "fetchForecastFallback")
    public OpenMeteoResponseDto fetchForecast(double latitude, double longitude, int pastDays, int forecastDays) {
        log.info("Rozpoczęcie pobierania prognozy Open-Meteo: lat={}, lon={}, pastDays={}, forecastDays={}",
                latitude, longitude, pastDays, forecastDays);
        try {
            return openMeteoRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/forecast")
                            .queryParam("latitude", latitude)
                            .queryParam("longitude", longitude)
                            .queryParam("hourly", "temperature_2m,relative_humidity_2m,precipitation,surface_pressure,pressure_msl,wind_speed_10m,wind_direction_10m")
                            .queryParam("wind_speed_unit", "ms")
                            .queryParam("timezone", "Europe/Warsaw")
                            .queryParam("past_days", pastDays)
                            .queryParam("forecast_days", forecastDays)
                            .build())
                    .retrieve()
                    .body(OpenMeteoResponseDto.class);
        } catch (RestClientException e) {
            log.error("Błąd podczas komunikacji z API Open-Meteo (lat={}, lon={}): {}", latitude, longitude, e.getMessage(), e);
            throw new WeatherApiClientException("Nie udało się pobrać danych prognozy z Open-Meteo", e);
        }
    }

    private OpenMeteoResponseDto fetchForecastFallback(double latitude, double longitude, int pastDays, int forecastDays, Exception e) {
        log.warn("Circuit Breaker otwarty dla Open-Meteo (lat={}, lon={}). Powód: {}", latitude, longitude, e.getMessage());
        return null;
    }
}
