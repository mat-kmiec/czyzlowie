package pl.matkmiec.backend.weather.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pl.matkmiec.backend.weather.model.ImgwSynopStation;
import pl.matkmiec.backend.weather.model.OpenMeteoForecast;
import pl.matkmiec.backend.weather.repository.ImgwHydroStationRepository;
import pl.matkmiec.backend.weather.repository.ImgwMeteoStationRepository;
import pl.matkmiec.backend.weather.repository.ImgwSynopStationRepository;
import pl.matkmiec.backend.weather.repository.OpenMeteoForecastRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class WeatherPublicControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ImgwSynopStationRepository synopStationRepo;

    @Mock
    private ImgwMeteoStationRepository meteoStationRepo;

    @Mock
    private ImgwHydroStationRepository hydroStationRepo;

    @Mock
    private OpenMeteoForecastRepository openMeteoForecastRepo;

    @InjectMocks
    private WeatherPublicController weatherPublicController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(weatherPublicController).build();
    }

    @Test
    void shouldReturnActiveSynopStations() throws Exception {
        ImgwSynopStation active = ImgwSynopStation.builder().id("11111").name("Active Station").isActive(true).build();
        when(synopStationRepo.findAllByIsActiveTrue()).thenReturn(List.of(active));

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

        when(openMeteoForecastRepo.findAllBySynopStation_IdAndForecastTimeAfterOrderByForecastTimeAsc(eq("12295"), any(LocalDateTime.class)))
                .thenReturn(List.of(forecast));

        mockMvc.perform(get("/public/weather/forecast/12295"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].temperature").value(18.5))
                .andExpect(jsonPath("$[0].windSpeed").value(3.2));
    }
}
