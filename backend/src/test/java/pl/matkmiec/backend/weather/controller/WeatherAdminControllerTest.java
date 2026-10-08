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
import pl.matkmiec.backend.weather.model.ImgwSynopStation;
import pl.matkmiec.backend.weather.model.WeatherSyncLog;
import pl.matkmiec.backend.weather.service.StationManagementService;
import pl.matkmiec.backend.weather.service.SyncLogService;
import pl.matkmiec.backend.weather.service.WeatherCleanupService;
import pl.matkmiec.backend.weather.service.WeatherSyncService;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
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

    @Mock
    private WeatherCleanupService weatherCleanupService;

    @InjectMocks
    private WeatherAdminController weatherAdminController;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.registerModule(new SpringDataJacksonConfiguration.PageModule(new SpringDataWebSettings(EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)));

        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper);

        mockMvc = MockMvcBuilders.standaloneSetup(weatherAdminController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(converter)
                .build();
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

    @Test
    void shouldGetSyncLogsWithPagination() throws Exception {
        WeatherSyncLog log = WeatherSyncLog.builder().id(123L).providerName("IMGW_SYNOP").status("SUCCESS").build();
        when(syncLogService.getLogs(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(log)));

        mockMvc.perform(get("/admin/weather/sync/logs").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(123))
                .andExpect(jsonPath("$.content[0].providerName").value("IMGW_SYNOP"));
    }

    @Test
    void shouldTriggerCleanup() throws Exception {
        when(weatherCleanupService.cleanupOldData()).thenReturn(Map.of(
                "status", "SUCCESS",
                "totalDeleted", 15
        ));

        mockMvc.perform(post("/admin/weather/cleanup"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.totalDeleted").value(15));
    }

    @Test
    void shouldTriggerCleanupWithCustomDays() throws Exception {
        when(weatherCleanupService.cleanupDataOlderThan(10)).thenReturn(Map.of(
                "status", "SUCCESS",
                "retentionDays", 10,
                "totalDeleted", 5
        ));

        mockMvc.perform(post("/admin/weather/cleanup").param("retentionDays", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.retentionDays").value(10))
                .andExpect(jsonPath("$.totalDeleted").value(5));
    }
}
