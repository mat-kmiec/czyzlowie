package pl.matkmiec.backend.airquality.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import pl.matkmiec.backend.airquality.exception.AirQualityApiException;

import java.util.Collections;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class IosApiClient {

    private final RestClient iosRestClient;

    @CircuitBreaker(name = "default", fallbackMethod = "fallbackForecast")
    @RateLimiter(name = "default")
    public Map<String, Double> fetchForecast(String pollutant, String terytCode) {
        try {
            Map<String, Double> forecast = iosRestClient.get()
                    .uri("/{pollutant}/{teryt}", pollutant, terytCode)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Double>>() {});
            return forecast != null ? forecast : Collections.emptyMap();
        } catch (Exception e) {
            log.error("Błąd podczas pobierania prognozy IOŚ dla zanieczyszczenia {} i TERYT {}: {}", pollutant, terytCode, e.getMessage());
            throw new AirQualityApiException("Nie udało się pobrać prognozy IOŚ dla " + pollutant + " i TERYT: " + terytCode, e);
        }
    }

    public Map<String, Double> fallbackForecast(String pollutant, String terytCode, Throwable t) {
        log.warn("Fallback dla prognozy IOŚ ({}, {}) z powodu: {}", pollutant, terytCode, t.getMessage());
        return Collections.emptyMap();
    }
}
