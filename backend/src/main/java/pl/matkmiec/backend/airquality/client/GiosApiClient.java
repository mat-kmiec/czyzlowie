package pl.matkmiec.backend.airquality.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import pl.matkmiec.backend.airquality.dto.GiosIndexResponseDto;
import pl.matkmiec.backend.airquality.dto.GiosMeasurementDataResponseDto;
import pl.matkmiec.backend.airquality.dto.GiosSensorResponseDto;
import pl.matkmiec.backend.airquality.dto.GiosStationResponseDto;
import pl.matkmiec.backend.airquality.exception.AirQualityApiException;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GiosApiClient {

    private final RestClient giosRestClient;

    @CircuitBreaker(name = "default", fallbackMethod = "fallbackStations")
    @RateLimiter(name = "default")
    public List<GiosStationResponseDto> fetchAllStations() {
        try {
            List<GiosStationResponseDto> stations = giosRestClient.get()
                    .uri("/station/findAll")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<GiosStationResponseDto>>() {});
            return stations != null ? stations : Collections.emptyList();
        } catch (Exception e) {
            log.error("Błąd podczas pobierania stacji pomiarowych z GIOŚ API: {}", e.getMessage());
            throw new AirQualityApiException("Nie udało się pobrać stacji pomiarowych z GIOŚ", e);
        }
    }

    @CircuitBreaker(name = "default", fallbackMethod = "fallbackSensors")
    @RateLimiter(name = "default")
    public List<GiosSensorResponseDto> fetchSensors(Long stationId) {
        try {
            List<GiosSensorResponseDto> sensors = giosRestClient.get()
                    .uri("/station/sensors/{stationId}", stationId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<GiosSensorResponseDto>>() {});
            return sensors != null ? sensors : Collections.emptyList();
        } catch (Exception e) {
            log.error("Błąd podczas pobierania stanowisk pomiarowych dla stacji {} z GIOŚ API: {}", stationId, e.getMessage());
            throw new AirQualityApiException("Nie udało się pobrać stanowisk pomiarowych dla stacji: " + stationId, e);
        }
    }

    @CircuitBreaker(name = "default", fallbackMethod = "fallbackMeasurementData")
    @RateLimiter(name = "default")
    public GiosMeasurementDataResponseDto fetchMeasurementData(Long sensorId) {
        try {
            return giosRestClient.get()
                    .uri("/data/getData/{sensorId}", sensorId)
                    .retrieve()
                    .body(GiosMeasurementDataResponseDto.class);
        } catch (Exception e) {
            log.error("Błąd podczas pobierania danych pomiarowych ze stanowiska {} z GIOŚ API: {}", sensorId, e.getMessage());
            throw new AirQualityApiException("Nie udało się pobrać danych pomiarowych ze stanowiska: " + sensorId, e);
        }
    }

    @CircuitBreaker(name = "default", fallbackMethod = "fallbackAirQualityIndex")
    @RateLimiter(name = "default")
    public GiosIndexResponseDto fetchAirQualityIndex(Long stationId) {
        try {
            return giosRestClient.get()
                    .uri("/aqindex/getIndex/{stationId}", stationId)
                    .retrieve()
                    .body(GiosIndexResponseDto.class);
        } catch (Exception e) {
            log.error("Błąd podczas pobierania indeksu jakości powietrza dla stacji {} z GIOŚ API: {}", stationId, e.getMessage());
            throw new AirQualityApiException("Nie udało się pobrać indeksu jakości powietrza dla stacji: " + stationId, e);
        }
    }

    public List<GiosStationResponseDto> fallbackStations(Throwable t) {
        log.warn("Fallback dla fetchAllStations z powodu: {}", t.getMessage());
        return Collections.emptyList();
    }

    public List<GiosSensorResponseDto> fallbackSensors(Long stationId, Throwable t) {
        log.warn("Fallback dla fetchSensors (stacja {}) z powodu: {}", stationId, t.getMessage());
        return Collections.emptyList();
    }

    public GiosMeasurementDataResponseDto fallbackMeasurementData(Long sensorId, Throwable t) {
        log.warn("Fallback dla fetchMeasurementData (stanowisko {}) z powodu: {}", sensorId, t.getMessage());
        return null;
    }

    public GiosIndexResponseDto fallbackAirQualityIndex(Long stationId, Throwable t) {
        log.warn("Fallback dla fetchAirQualityIndex (stacja {}) z powodu: {}", stationId, t.getMessage());
        return null;
    }
}
