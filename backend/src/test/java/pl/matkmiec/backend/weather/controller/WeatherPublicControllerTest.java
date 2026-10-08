package pl.matkmiec.backend.weather.controller;

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
import pl.matkmiec.backend.weather.dto.CurrentWeatherDto;
import pl.matkmiec.backend.weather.dto.NearestStationDto;
import pl.matkmiec.backend.weather.dto.NearestStationsOverviewDto;
import pl.matkmiec.backend.weather.model.ImgwSynopData;
import pl.matkmiec.backend.weather.model.ImgwSynopStation;
import pl.matkmiec.backend.weather.model.OpenMeteoForecast;
import pl.matkmiec.backend.weather.service.WeatherQueryService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class WeatherPublicControllerTest {

    private MockMvc mockMvc;

    @Mock
    private WeatherQueryService weatherQueryService;

    @InjectMocks
    private WeatherPublicController weatherPublicController;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.registerModule(new SpringDataJacksonConfiguration.PageModule(new SpringDataWebSettings(EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)));

        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper);

        mockMvc = MockMvcBuilders.standaloneSetup(weatherPublicController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(converter)
                .build();
    }

    @Test
    void shouldReturnActiveSynopStations() throws Exception {
        ImgwSynopStation active = ImgwSynopStation.builder().id("11111").name("Active Station").isActive(true).build();
        when(weatherQueryService.getActiveSynopStations()).thenReturn(List.of(active));

        mockMvc.perform(get("/public/weather/stations/synop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value("11111"));
    }

    @Test
    void shouldReturnForecastForStation() throws Exception {
        ImgwSynopStation station = ImgwSynopStation.builder().id("12295").name("Białystok").isActive(true).build();
        OpenMeteoForecast forecast = OpenMeteoForecast.builder()
                .id(1L)
                .synopStation(station)
                .forecastTime(LocalDateTime.of(2026, 10, 8, 15, 0))
                .generatedAt(LocalDateTime.of(2026, 10, 8, 12, 0))
                .temperature(18.5)
                .windSpeed(3.2)
                .windDirection(180)
                .relativeHumidity(65.0)
                .precipitation(0.0)
                .pressure(1015.0)
                .build();

        when(weatherQueryService.getForecastForStation(eq("12295"))).thenReturn(List.of(forecast));

        mockMvc.perform(get("/public/weather/forecast/12295"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].temperature").value(18.5))
                .andExpect(jsonPath("$[0].windSpeed").value(3.2));
    }

    @Test
    void shouldReturnNearestStation() throws Exception {
        NearestStationDto nearest = NearestStationDto.builder()
                .id("12375")
                .name("Warszawa-Okęcie")
                .stationType("SYNOP")
                .lat(52.16)
                .lon(20.96)
                .distanceKm(4.2)
                .build();

        when(weatherQueryService.findNearestStation(eq(52.23), eq(21.01), eq("SYNOP"))).thenReturn(nearest);

        mockMvc.perform(get("/public/weather/nearest")
                        .param("lat", "52.23")
                        .param("lon", "21.01")
                        .param("type", "SYNOP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("12375"))
                .andExpect(jsonPath("$.name").value("Warszawa-Okęcie"))
                .andExpect(jsonPath("$.distanceKm").value(4.2));
    }

    @Test
    void shouldReturnNearestStationsOverview() throws Exception {
        NearestStationsOverviewDto overview = NearestStationsOverviewDto.builder()
                .nearestSynop(NearestStationDto.builder().id("12375").name("Warszawa").stationType("SYNOP").distanceKm(4.2).build())
                .nearestMeteo(NearestStationDto.builder().id("252200150").name("Warszawa-Bielany").stationType("METEO").distanceKm(8.1).build())
                .nearestHydro(NearestStationDto.builder().id("152210010").name("Warszawa-Nadwilanówka").stationType("HYDRO").distanceKm(12.0).build())
                .build();

        when(weatherQueryService.findNearestStationsOverview(eq(52.23), eq(21.01))).thenReturn(overview);

        mockMvc.perform(get("/public/weather/nearest/overview")
                        .param("lat", "52.23")
                        .param("lon", "21.01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nearestSynop.id").value("12375"))
                .andExpect(jsonPath("$.nearestMeteo.id").value("252200150"))
                .andExpect(jsonPath("$.nearestHydro.id").value("152210010"));
    }

    @Test
    void shouldReturnCurrentWeatherByStationId() throws Exception {
        CurrentWeatherDto current = CurrentWeatherDto.builder()
                .stationId("12375")
                .stationName("Warszawa-Okęcie")
                .temperature(21.4)
                .windSpeed(4.1)
                .pressure(1013.2)
                .build();

        when(weatherQueryService.getCurrentWeatherByStationId("12375")).thenReturn(current);

        mockMvc.perform(get("/public/weather/current/12375"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stationId").value("12375"))
                .andExpect(jsonPath("$.temperature").value(21.4));
    }

    @Test
    void shouldReturnCurrentWeatherByCoordinates() throws Exception {
        CurrentWeatherDto current = CurrentWeatherDto.builder()
                .stationId("12375")
                .stationName("Warszawa-Okęcie")
                .distanceKm(4.2)
                .temperature(21.4)
                .build();

        when(weatherQueryService.getCurrentWeatherByCoordinates(52.23, 21.01)).thenReturn(current);

        mockMvc.perform(get("/public/weather/current")
                        .param("lat", "52.23")
                        .param("lon", "21.01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stationId").value("12375"))
                .andExpect(jsonPath("$.distanceKm").value(4.2));
    }

    @Test
    void shouldReturnPaginatedSynopHistory() throws Exception {
        ImgwSynopData synop = ImgwSynopData.builder()
                .id(1L)
                .temperature(20.0)
                .measurementTime(LocalDateTime.of(2026, 10, 8, 14, 0))
                .build();

        when(weatherQueryService.getSynopHistory(isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(synop)));

        mockMvc.perform(get("/public/weather/history/synop")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].temperature").value(20.0));
    }
}
