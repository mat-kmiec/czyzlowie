package pl.matkmiec.backend.airquality.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import pl.matkmiec.backend.airquality.dto.GiosIndexResponseDto;
import pl.matkmiec.backend.airquality.dto.GiosMeasurementDataResponseDto;
import pl.matkmiec.backend.airquality.dto.GiosSensorResponseDto;
import pl.matkmiec.backend.airquality.dto.GiosStationResponseDto;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GiosApiClientTest {

    private MockRestServiceServer mockServer;
    private GiosApiClient giosApiClient;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.gios.gov.pl/pjp-api/v1/rest");
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        giosApiClient = new GiosApiClient(restClient, objectMapper);
    }

    @Test
    void shouldFetchAllStationsV1Format() {
        String json = """
                {
                  "Lista stacji pomiarowych": [
                    {
                      "Identyfikator stacji": 11,
                      "Kod stacji": "DsCzerStraza",
                      "Nazwa stacji": "Czerniawa",
                      "WGS84 φ N": "50.912475",
                      "WGS84 λ E": "15.312190",
                      "Identyfikator miasta": 142,
                      "Nazwa miasta": "Czerniawa",
                      "Gmina": "Świeradów-Zdrój",
                      "Powiat": "lubański",
                      "Województwo": "DOLNOŚLĄSKIE",
                      "Ulica": "ul. Strażacka 7"
                    }
                  ],
                  "totalPages": 1
                }
                """;

        mockServer.expect(requestTo("https://api.gios.gov.pl/pjp-api/v1/rest/station/findAll?page=0&size=500"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        List<GiosStationResponseDto> stations = giosApiClient.fetchAllStations();

        assertNotNull(stations);
        assertEquals(1, stations.size());
        GiosStationResponseDto station = stations.get(0);
        assertEquals(11L, station.id());
        assertEquals("Czerniawa", station.stationName());
        assertEquals(50.912475, station.gegrLat());
        assertEquals(15.312190, station.gegrLon());
        assertEquals(142L, station.cityId());
        assertEquals("Czerniawa", station.cityName());
        assertEquals("Świeradów-Zdrój", station.communeName());
        assertEquals("lubański", station.districtName());
        assertEquals("DOLNOŚLĄSKIE", station.provinceName());
        assertEquals("ul. Strażacka 7", station.addressStreet());
        mockServer.verify();
    }

    @Test
    void shouldFetchSensorsV1Format() {
        String json = """
                {
                  "Lista stanowisk pomiarowych dla podanej stacji": [
                    {
                      "Identyfikator stanowiska": 54,
                      "Identyfikator stacji": 11,
                      "Wskaźnik": "pył zawieszony PM10",
                      "Wskaźnik - wzór": "PM10",
                      "Wskaźnik - kod": "PM10",
                      "Id wskaźnika": 3
                    }
                  ],
                  "totalPages": 1
                }
                """;

        mockServer.expect(requestTo("https://api.gios.gov.pl/pjp-api/v1/rest/station/sensors/11"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        List<GiosSensorResponseDto> sensors = giosApiClient.fetchSensors(11L);

        assertNotNull(sensors);
        assertEquals(1, sensors.size());
        GiosSensorResponseDto sensor = sensors.get(0);
        assertEquals(54L, sensor.id());
        assertEquals(11L, sensor.stationId());
        assertEquals("PM10", sensor.paramCode());
        assertEquals("PM10", sensor.paramFormula());
        assertEquals("pył zawieszony PM10", sensor.paramName());
        assertEquals(3, sensor.idParam());
        mockServer.verify();
    }

    @Test
    void shouldFetchMeasurementDataV1Format() {
        String json = """
                {
                  "Lista danych pomiarowych": [
                    {
                      "Kod stanowiska": "DsCzerStraza-PM10-1g",
                      "Data": "2026-10-13 12:00:00",
                      "Wartość": 5.8
                    },
                    {
                      "Kod stanowiska": "DsCzerStraza-PM10-1g",
                      "Data": "2026-10-13 11:00:00",
                      "Wartość": 4.8
                    }
                  ],
                  "totalPages": 1
                }
                """;

        mockServer.expect(requestTo("https://api.gios.gov.pl/pjp-api/v1/rest/data/getData/54?size=100"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        GiosMeasurementDataResponseDto data = giosApiClient.fetchMeasurementData(54L);

        assertNotNull(data);
        assertEquals("DsCzerStraza-PM10-1g", data.key());
        assertEquals(2, data.values().size());
        assertEquals("2026-10-13 12:00:00", data.values().get(0).date());
        assertEquals(5.8, data.values().get(0).value());
        mockServer.verify();
    }

    @Test
    void shouldFetchAirQualityIndexV1Format() {
        String json = """
                {
                  "AqIndex": {
                    "Identyfikator stacji pomiarowej": 11,
                    "Data wykonania obliczeń indeksu": "2026-10-13 13:20:28",
                    "Wartość indeksu": 0,
                    "Nazwa kategorii indeksu": "Bardzo dobry",
                    "Data danych źródłowych, z których policzono wartość indeksu dla wskaźnika st": "2026-10-13 12:00:00",
                    "Data wykonania obliczeń indeksu dla wskaźnika PM10": "2026-10-13 13:20:28",
                    "Wartość indeksu dla wskaźnika PM10": 0,
                    "Nazwa kategorii indeksu dla wskażnika PM10": "Bardzo dobry",
                    "Data danych źródłowych, z których policzono wartość indeksu dla wskaźnika PM10": "2026-10-13 12:00:00",
                    "Status indeksu ogólnego dla stacji pomiarowej": true,
                    "Kod zanieczyszczenia krytycznego": "OZON"
                  }
                }
                """;

        mockServer.expect(requestTo("https://api.gios.gov.pl/pjp-api/v1/rest/aqindex/getIndex/11"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        GiosIndexResponseDto index = giosApiClient.fetchAirQualityIndex(11L);

        assertNotNull(index);
        assertEquals(11L, index.id());
        assertEquals("2026-10-13 13:20:28", index.stCalcDate());
        assertEquals(0, index.stIndexLevelId());
        assertEquals("Bardzo dobry", index.stIndexLevelName());
        assertEquals("OZON", index.stIndexCrParam());
        assertEquals(0, index.stIndexLevelId());
        assertEquals("Bardzo dobry", index.pm10IndexLevelName());
        assertTrue(index.stIndexStatus());
        mockServer.verify();
    }

    @Test
    void shouldReturnNullWhenMeasurementDataReturnsBadRequestForManualSensor() {
        String errorJson = """
                {
                  "error_result": "Przepraszamy, nie można zwrócić danych.",
                  "error_reason": "Dla stanowiska typu manualnego wyniki pomiarów nie są dostępne na bieżąco, są one udostępniane po 4-8 tygodniach od poboru próby przez usługę API „Archiwalne dane pomiarowe”",
                  "error_solution": "Prosimy o podanie wartości parametrów zgodnych z dokumentacją.",
                  "error_help": "https://powietrze.gios.gov.pl/pjp/content/api",
                  "error_code": "API-ERR-100003"
                }
                """;

        mockServer.expect(requestTo("https://api.gios.gov.pl/pjp-api/v1/rest/data/getData/6028?size=100"))
                .andRespond(withBadRequest().body(errorJson).contentType(MediaType.APPLICATION_JSON));

        GiosMeasurementDataResponseDto data = giosApiClient.fetchMeasurementData(6028L);

        assertNull(data);
        mockServer.verify();
    }

    @Test
    void shouldReturnNullWhenMeasurementDataReturnsNotFound() {
        mockServer.expect(requestTo("https://api.gios.gov.pl/pjp-api/v1/rest/data/getData/9999?size=100"))
                .andRespond(withResourceNotFound());

        GiosMeasurementDataResponseDto data = giosApiClient.fetchMeasurementData(9999L);

        assertNull(data);
        mockServer.verify();
    }

    @Test
    void shouldReturnNullWhenAirQualityIndexReturnsNotFoundOrBadRequest() {
        mockServer.expect(requestTo("https://api.gios.gov.pl/pjp-api/v1/rest/aqindex/getIndex/9999"))
                .andRespond(withResourceNotFound());

        GiosIndexResponseDto index = giosApiClient.fetchAirQualityIndex(9999L);

        assertNull(index);
        mockServer.verify();
    }

    @Test
    void shouldReturnEmptyListWhenSensorsReturnsNotFound() {
        mockServer.expect(requestTo("https://api.gios.gov.pl/pjp-api/v1/rest/station/sensors/9999"))
                .andRespond(withResourceNotFound());

        List<GiosSensorResponseDto> sensors = giosApiClient.fetchSensors(9999L);

        assertNotNull(sensors);
        assertTrue(sensors.isEmpty());
        mockServer.verify();
    }
}
