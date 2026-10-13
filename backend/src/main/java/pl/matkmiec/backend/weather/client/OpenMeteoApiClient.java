package pl.matkmiec.backend.weather.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
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

    @RateLimiter(name = "openMeteo")
    @Retry(name = "openMeteo", fallbackMethod = "fetchForecastFallback")
    @CircuitBreaker(name = "openMeteo", fallbackMethod = "fetchForecastFallback")
    public OpenMeteoResponseDto fetchForecast(double latitude, double longitude, int pastDays, int forecastDays) {
        log.info("Rozpoczęcie pobierania prognozy Open-Meteo: lat={}, lon={}, pastDays={}, forecastDays={}",
                latitude, longitude, pastDays, forecastDays);
        try {
            return openMeteoRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/forecast")
                            .queryParam("latitude", latitude)
                            .queryParam("longitude", longitude)
                            .queryParam("hourly", "temperature_2m,relative_humidity_2m,precipitation,surface_pressure,pressure_msl,wind_speed_10m,wind_direction_10m,uv_index,weather_code,wind_gusts_10m,cloud_cover")
                            .queryParam("wind_speed_unit", "ms")
                            .queryParam("timezone", "Europe/Warsaw")
                            .queryParam("past_days", pastDays)
                            .queryParam("forecast_days", forecastDays)
                            .build())
                    .retrieve()
                    .body(OpenMeteoResponseDto.class);
        } catch (HttpClientErrorException.TooManyRequests e) {
            log.warn("Limit zapytań (429 Too Many Requests) dla API Open-Meteo (lat={}, lon={}): {}. Nastąpi ponowienie z opóźnieniem.",
                    latitude, longitude, e.getMessage());
            throw new WeatherApiClientException("Przekroczono limit zapytań do Open-Meteo (429)", e);
        } catch (RestClientException e) {
            log.error("Błąd podczas komunikacji z API Open-Meteo (lat={}, lon={}): {}", latitude, longitude, e.getMessage(), e);
            throw new WeatherApiClientException("Nie udało się pobrać danych prognozy z Open-Meteo", e);
        }
    }

    private OpenMeteoResponseDto fetchForecastFallback(double latitude, double longitude, int pastDays, int forecastDays, Throwable t) {
        log.warn("Circuit Breaker otwarty lub wyczerpano próby dla Open-Meteo (lat={}, lon={}). Powód: {}", latitude, longitude, t.getMessage());
        return null;
    }
}
