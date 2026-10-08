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
import pl.matkmiec.backend.weather.repository.ImgwHydroStationRepository;
import pl.matkmiec.backend.weather.repository.ImgwMeteoStationRepository;
import pl.matkmiec.backend.weather.repository.ImgwSynopStationRepository;

import java.util.List;

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
}
