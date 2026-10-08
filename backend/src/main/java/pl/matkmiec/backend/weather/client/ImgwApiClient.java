package pl.matkmiec.backend.weather.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import pl.matkmiec.backend.weather.dto.ImgwHydroResponseDto;
import pl.matkmiec.backend.weather.dto.ImgwMeteoResponseDto;
import pl.matkmiec.backend.weather.dto.ImgwSynopResponseDto;
import pl.matkmiec.backend.weather.exception.WeatherApiClientException;

import java.util.List;

@Slf4j
@Component
public class ImgwApiClient {

    private final RestClient imgwRestClient;

    public ImgwApiClient(@Qualifier("imgwRestClient") RestClient imgwRestClient) {
        this.imgwRestClient = imgwRestClient;
    }

    @Retry(name = "default")
    @CircuitBreaker(name = "default", fallbackMethod = "fetchSynopFallback")
    public List<ImgwSynopResponseDto> fetchSynop() {
        log.info("Rozpoczęcie pobierania danych Synop z IMGW...");
        try {
            return imgwRestClient.get()
                    .uri("/data/synop")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<ImgwSynopResponseDto>>() {});
        } catch (RestClientException e) {
            log.error("Błąd podczas komunikacji z API IMGW Synop: {}", e.getMessage(), e);
            throw new WeatherApiClientException("Nie udało się pobrać danych Synop z IMGW", e);
        }
    }

    @Retry(name = "default")
    @CircuitBreaker(name = "default", fallbackMethod = "fetchHydroFallback")
    public List<ImgwHydroResponseDto> fetchHydro() {
        log.info("Rozpoczęcie pobierania danych Hydro z IMGW...");
        try {
            return imgwRestClient.get()
                    .uri("/data/hydro")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<ImgwHydroResponseDto>>() {});
        } catch (RestClientException e) {
            log.error("Błąd podczas komunikacji z API IMGW Hydro: {}", e.getMessage(), e);
            throw new WeatherApiClientException("Nie udało się pobrać danych Hydro z IMGW", e);
        }
    }

    private List<ImgwSynopResponseDto> fetchSynopFallback(Exception e) {
        log.warn("Circuit Breaker otwarty. Zwracam pustą listę dla IMGW Synop. Powód: {}", e.getMessage());
        return List.of();
    }

    private List<ImgwHydroResponseDto> fetchHydroFallback(Exception e) {
        log.warn("Circuit Breaker otwarty. Zwracam pustą listę dla IMGW Hydro. Powód: {}", e.getMessage());
        return List.of();
    }

    @Retry(name = "default")
    @CircuitBreaker(name = "default", fallbackMethod = "fetchMeteoFallback")
    public List<ImgwMeteoResponseDto> fetchMeteo() {
        log.info("Rozpoczęcie pobierania danych Meteo z IMGW...");
        try {
            return imgwRestClient.get()
                    .uri("/data/meteo")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<ImgwMeteoResponseDto>>() {});
        } catch (RestClientException e) {
            log.error("Błąd podczas komunikacji z API IMGW Meteo: {}", e.getMessage(), e);
            throw new WeatherApiClientException("Nie udało się pobrać danych Meteo z IMGW", e);
        }
    }

    private List<ImgwMeteoResponseDto> fetchMeteoFallback(Exception e) {
        log.warn("Circuit Breaker otwarty. Zwracam pustą listę dla IMGW Meteo. Powód: {}", e.getMessage());
        return List.of();
    }
}
