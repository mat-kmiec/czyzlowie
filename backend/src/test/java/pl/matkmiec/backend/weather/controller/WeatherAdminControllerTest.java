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
import pl.matkmiec.backend.weather.service.StationManagementService;
import pl.matkmiec.backend.weather.service.SyncLogService;
import pl.matkmiec.backend.weather.service.WeatherSyncService;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class WeatherAdminControllerTest {

    private MockMvc mockMvc;

    @Mock
    private WeatherSyncService weatherSyncService;

    @Mock
    private StationManagementService stationManagementService;

    @Mock
    private SyncLogService syncLogService;

    @InjectMocks
    private WeatherAdminController weatherAdminController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(weatherAdminController).build();
    }

    @Test
    void shouldTriggerSynopSync() throws Exception {
        when(weatherSyncService.syncSynopData()).thenReturn(10);

        mockMvc.perform(post("/admin/weather/sync/synop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("IMGW_SYNOP"))
                .andExpect(jsonPath("$.recordsProcessed").value(10));
    }

    @Test
    void shouldTriggerOpenMeteoSync() throws Exception {
        when(weatherSyncService.syncOpenMeteoData()).thenReturn(48);

        mockMvc.perform(post("/admin/weather/sync/open-meteo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("OPEN_METEO"))
                .andExpect(jsonPath("$.recordsProcessed").value(48));
    }

    @Test
    void shouldToggleSynopStation() throws Exception {
        when(stationManagementService.toggleSynopStationActive("12345")).thenReturn(false);

        mockMvc.perform(patch("/admin/weather/stations/synop/12345/toggle-active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stationId").value("12345"))
                .andExpect(jsonPath("$.isActive").value(false));
    }

    @Test
    void shouldGetStations() throws Exception {
        ImgwSynopStation s = ImgwSynopStation.builder().id("123").name("Test").isActive(true).build();
        when(stationManagementService.getAllSynopStations()).thenReturn(List.of(s));

        mockMvc.perform(get("/admin/weather/stations/synop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("123"));
    }
}
