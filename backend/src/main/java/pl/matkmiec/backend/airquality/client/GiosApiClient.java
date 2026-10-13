package pl.matkmiec.backend.airquality.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import pl.matkmiec.backend.airquality.dto.GiosIndexResponseDto;
import pl.matkmiec.backend.airquality.dto.GiosMeasurementDataResponseDto;
import pl.matkmiec.backend.airquality.dto.GiosMeasurementValueDto;
import pl.matkmiec.backend.airquality.dto.GiosSensorResponseDto;
import pl.matkmiec.backend.airquality.dto.GiosStationResponseDto;
import pl.matkmiec.backend.airquality.exception.AirQualityApiException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GiosApiClient {

    private final RestClient giosRestClient;
    private final ObjectMapper objectMapper;

    @CircuitBreaker(name = "gios", fallbackMethod = "fallbackStations")
    @RateLimiter(name = "gios")
    @Retry(name = "gios")
    public List<GiosStationResponseDto> fetchAllStations() {
        try {
            List<GiosStationResponseDto> allStations = new ArrayList<>();
            int page = 0;
            int totalPages = 1;

            while (page < totalPages) {
                String rawJson = giosRestClient.get()
                        .uri("/station/findAll?page={page}&size=500", page)
                        .retrieve()
                        .body(String.class);

                if (rawJson == null || rawJson.trim().isEmpty()) {
                    break;
                }

                JsonNode root = objectMapper.readTree(rawJson);

                if (root.isArray()) {
                    List<GiosStationResponseDto> list = objectMapper.readerForListOf(GiosStationResponseDto.class).readValue(root);
                    if (list != null) {
                        allStations.addAll(list);
                    }
                    break;
                } else if (root.isObject()) {
                    JsonNode arrayNode = null;
                    if (root.has("Lista stacji pomiarowych")) {
                        arrayNode = root.get("Lista stacji pomiarowych");
                    } else if (root.has("stationList")) {
                        arrayNode = root.get("stationList");
                    } else if (root.has("stations")) {
                        arrayNode = root.get("stations");
                    } else if (root.has("Lista stacji pomiarów")) {
                        arrayNode = root.get("Lista stacji pomiarów");
                    }

                    if (arrayNode != null && arrayNode.isArray()) {
                        List<GiosStationResponseDto> list = objectMapper.readerForListOf(GiosStationResponseDto.class).readValue(arrayNode);
                        if (list != null) {
                            allStations.addAll(list);
                        }
                    }

                    if (root.has("totalPages")) {
                        totalPages = root.get("totalPages").asInt(1);
                    }
                    page++;
                } else {
                    break;
                }
            }

            return allStations;
        } catch (Exception e) {
            log.error("Błąd podczas pobierania stacji pomiarowych z GIOŚ API: {}", e.getMessage());
            throw new AirQualityApiException("Nie udało się pobrać stacji pomiarowych z GIOŚ", e);
        }
    }

    @CircuitBreaker(name = "gios", fallbackMethod = "fallbackSensors")
    @RateLimiter(name = "gios")
    @Retry(name = "gios")
    public List<GiosSensorResponseDto> fetchSensors(Long stationId) {
        try {
            String rawJson = giosRestClient.get()
                    .uri("/station/sensors/{stationId}", stationId)
                    .retrieve()
                    .body(String.class);

            if (rawJson == null || rawJson.trim().isEmpty()) {
                return Collections.emptyList();
            }

            JsonNode root = objectMapper.readTree(rawJson);

            if (root.isArray()) {
                return objectMapper.readerForListOf(GiosSensorResponseDto.class).readValue(root);
            } else if (root.isObject()) {
                JsonNode arrayNode = null;
                if (root.has("Lista stanowisk pomiarowych dla podanej stacji")) {
                    arrayNode = root.get("Lista stanowisk pomiarowych dla podanej stacji");
                } else if (root.has("sensors")) {
                    arrayNode = root.get("sensors");
                } else if (root.has("sensorList")) {
                    arrayNode = root.get("sensorList");
                }

                if (arrayNode != null && arrayNode.isArray()) {
                    return objectMapper.readerForListOf(GiosSensorResponseDto.class).readValue(arrayNode);
                }
            }

            return Collections.emptyList();
        } catch (HttpClientErrorException.NotFound e) {
            log.debug("Brak czujników dla stacji {} z GIOŚ API: {}", stationId, e.getMessage());
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Błąd podczas pobierania stanowisk pomiarowych dla stacji {} z GIOŚ API: {}", stationId, e.getMessage());
            throw new AirQualityApiException("Nie udało się pobrać stanowisk pomiarowych dla stacji: " + stationId, e);
        }
    }

    @CircuitBreaker(name = "gios", fallbackMethod = "fallbackMeasurementData")
    @RateLimiter(name = "gios")
    @Retry(name = "gios")
    public GiosMeasurementDataResponseDto fetchMeasurementData(Long sensorId) {
        try {
            String rawJson = giosRestClient.get()
                    .uri("/data/getData/{sensorId}?size=100", sensorId)
                    .retrieve()
                    .body(String.class);

            if (rawJson == null || rawJson.trim().isEmpty()) {
                return null;
            }

            JsonNode root = objectMapper.readTree(rawJson);

            if (root.has("values")) {
                return objectMapper.treeToValue(root, GiosMeasurementDataResponseDto.class);
            }

            if (root.has("Lista danych pomiarowych")) {
                JsonNode listNode = root.get("Lista danych pomiarowych");
                List<GiosMeasurementValueDto> values = objectMapper.readerForListOf(GiosMeasurementValueDto.class).readValue(listNode);
                String key = null;
                if (listNode.isArray() && !listNode.isEmpty()) {
                    JsonNode first = listNode.get(0);
                    if (first.has("Kod stanowiska")) {
                        key = first.get("Kod stanowiska").asText(null);
                    }
                }
                return new GiosMeasurementDataResponseDto(key, values);
            }

            return objectMapper.treeToValue(root, GiosMeasurementDataResponseDto.class);
        } catch (HttpClientErrorException.BadRequest | HttpClientErrorException.NotFound e) {
            log.debug("Brak bieżących danych pomiarowych ze stanowiska {} z GIOŚ API (stanowisko manualne lub brak danych): {}", sensorId, e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Błąd podczas pobierania danych pomiarowych ze stanowiska {} z GIOŚ API: {}", sensorId, e.getMessage());
            throw new AirQualityApiException("Nie udało się pobrać danych pomiarowych ze stanowiska: " + sensorId, e);
        }
    }

    @CircuitBreaker(name = "gios", fallbackMethod = "fallbackAirQualityIndex")
    @RateLimiter(name = "gios")
    @Retry(name = "gios")
    public GiosIndexResponseDto fetchAirQualityIndex(Long stationId) {
        try {
            String rawJson = giosRestClient.get()
                    .uri("/aqindex/getIndex/{stationId}", stationId)
                    .retrieve()
                    .body(String.class);

            if (rawJson == null || rawJson.trim().isEmpty()) {
                return null;
            }

            JsonNode root = objectMapper.readTree(rawJson);

            if (root.has("AqIndex")) {
                JsonNode indexNode = root.get("AqIndex");
                return objectMapper.treeToValue(indexNode, GiosIndexResponseDto.class);
            }

            return objectMapper.treeToValue(root, GiosIndexResponseDto.class);
        } catch (HttpClientErrorException.BadRequest | HttpClientErrorException.NotFound e) {
            log.debug("Brak indeksu jakości powietrza dla stacji {} z GIOŚ API: {}", stationId, e.getMessage());
            return null;
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
