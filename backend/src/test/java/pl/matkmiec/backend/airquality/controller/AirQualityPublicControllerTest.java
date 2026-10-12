package pl.matkmiec.backend.airquality.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.SpringDataJacksonConfiguration;
import org.springframework.data.web.config.SpringDataWebSettings;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pl.matkmiec.backend.airquality.dto.*;
import pl.matkmiec.backend.airquality.model.GiosMeasurement;
import pl.matkmiec.backend.airquality.model.GiosStation;
import pl.matkmiec.backend.airquality.service.AirQualityQueryService;
import pl.matkmiec.backend.airquality.service.AirQualityStationManagementService;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AirQualityPublicControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AirQualityQueryService airQualityQueryService;

    @Mock
    private AirQualityStationManagementService stationManagementService;

    @InjectMocks
    private AirQualityPublicController controller;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.registerModule(new SpringDataJacksonConfiguration.PageModule(new SpringDataWebSettings(EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)));

        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(converter)
                .build();
    }

    @Test
    void shouldReturnMapPoints() throws Exception {
        AirQualityMapPointDto point = new AirQualityMapPointDto(
                114L,
                "Warszawa-Kondratowicza",
                52.2909,
                21.0345,
                "ul. Kondratowicza",
                "Warszawa",
                "Warszawa",
                "MAZOWIECKIE",
                "1465",
                1,
                "Dobry",
                "PM10",
                Map.of("PM10", new PollutantMeasurementDto("PM10", "pył PM10", 24.5, "µg/m³", LocalDateTime.now(), "Dobry")),
                LocalDateTime.now()
        );

        when(airQualityQueryService.getMapPoints()).thenReturn(List.of(point));

        mockMvc.perform(get("/public/air-quality/map"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].stationId").value(114))
                .andExpect(jsonPath("$[0].stationName").value("Warszawa-Kondratowicza"))
                .andExpect(jsonPath("$[0].overallIndexLevelName").value("Dobry"));
    }

    @Test
    void shouldReturnRankings() throws Exception {
        AirQualityRankingDto ranking = new AirQualityRankingDto(
                1,
                114L,
                "Warszawa-Kondratowicza",
                "Warszawa",
                "Warszawa",
                "MAZOWIECKIE",
                52.2909,
                21.0345,
                "PM10",
                12.3,
                "µg/m³",
                0,
                "Bardzo dobry",
                LocalDateTime.now()
        );

        when(airQualityQueryService.getRankings(eq("PM10"), eq("BEST"), eq(10))).thenReturn(List.of(ranking));

        mockMvc.perform(get("/public/air-quality/rankings")
                        .param("pollutant", "PM10")
                        .param("order", "BEST")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].rank").value(1))
                .andExpect(jsonPath("$[0].stationId").value(114))
                .andExpect(jsonPath("$[0].value").value(12.3));
    }

    @Test
    void shouldReturnActiveStations() throws Exception {
        GiosStation station = GiosStation.builder().id(114L).stationName("Warszawa").isActive(true).build();
        when(stationManagementService.getActiveStations()).thenReturn(List.of(station));

        mockMvc.perform(get("/public/air-quality/stations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(114));
    }

    @Test
    void shouldReturnStationDetails() throws Exception {
        GiosStation station = GiosStation.builder().id(114L).stationName("Warszawa").build();
        AirQualityStationDetailsDto details = new AirQualityStationDetailsDto(
                station,
                null,
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyList(),
                Collections.emptyList()
        );

        when(airQualityQueryService.getStationDetails(114L)).thenReturn(details);

        mockMvc.perform(get("/public/air-quality/stations/114"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.station.id").value(114));
    }

    @Test
    void shouldReturnNearestStation() throws Exception {
        GiosStation station = GiosStation.builder().id(114L).stationName("Warszawa").build();
        NearestAirQualityStationDto nearest = new NearestAirQualityStationDto(
                station,
                3.5,
                1,
                "Dobry",
                "PM10",
                Collections.emptyMap()
        );

        when(airQualityQueryService.findNearestStation(52.23, 21.01)).thenReturn(nearest);

        mockMvc.perform(get("/public/air-quality/nearest")
                        .param("lat", "52.23")
                        .param("lon", "21.01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.distanceKm").value(3.5))
                .andExpect(jsonPath("$.overallIndexLevelName").value("Dobry"));
    }

    @Test
    void shouldReturnForecast() throws Exception {
        AirQualityForecastDto forecast = new AirQualityForecastDto("1465", "PM10", Collections.emptyList());
        when(airQualityQueryService.getForecast(eq("1465"), eq("PM10"))).thenReturn(List.of(forecast));

        mockMvc.perform(get("/public/air-quality/forecast")
                        .param("teryt", "1465")
                        .param("pollutant", "PM10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].terytCode").value("1465"))
                .andExpect(jsonPath("$[0].pollutant").value("PM10"));
    }

    @Test
    void shouldReturnHistory() throws Exception {
        GiosMeasurement m = GiosMeasurement.builder().id(1L).stationId(114L).paramCode("PM10").value(25.0).measurementTime(LocalDateTime.now()).build();
        when(airQualityQueryService.getMeasurementHistory(eq(114L), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(m)));

        mockMvc.perform(get("/public/air-quality/history")
                        .param("stationId", "114"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].value").value(25.0));
    }

    @Test
    void shouldReturnOverview() throws Exception {
        AirQualityOverviewDto overview = new AirQualityOverviewDto(
                200,
                180,
                Map.of("Dobry", 100L, "Bardzo dobry", 80L),
                Collections.emptyList(),
                Collections.emptyList(),
                LocalDateTime.now()
        );

        when(airQualityQueryService.getOverview()).thenReturn(overview);

        mockMvc.perform(get("/public/air-quality/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalStationsCount").value(200))
                .andExpect(jsonPath("$.activeStationsCount").value(180));
    }
}
