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
import pl.matkmiec.backend.airquality.model.AirQualitySyncLog;
import pl.matkmiec.backend.airquality.model.GiosStation;
import pl.matkmiec.backend.airquality.service.AirQualityCleanupService;
import pl.matkmiec.backend.airquality.service.AirQualityStationManagementService;
import pl.matkmiec.backend.airquality.service.AirQualitySyncLogService;
import pl.matkmiec.backend.airquality.service.AirQualitySyncService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AirQualityAdminControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AirQualitySyncService airQualitySyncService;

    @Mock
    private AirQualityStationManagementService stationManagementService;

    @Mock
    private AirQualitySyncLogService syncLogService;

    @Mock
    private AirQualityCleanupService cleanupService;

    @InjectMocks
    private AirQualityAdminController controller;

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
    void shouldTriggerStationsSync() throws Exception {
        when(airQualitySyncService.syncStations()).thenReturn(150);

        mockMvc.perform(post("/admin/air-quality/sync/stations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("GIOS_STATIONS"))
                .andExpect(jsonPath("$.recordsProcessed").value(150));
    }

    @Test
    void shouldTriggerSensorsSync() throws Exception {
        when(airQualitySyncService.syncSensors()).thenReturn(500);

        mockMvc.perform(post("/admin/air-quality/sync/sensors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("GIOS_SENSORS"))
                .andExpect(jsonPath("$.recordsProcessed").value(500));
    }

    @Test
    void shouldTriggerMeasurementsSync() throws Exception {
        when(airQualitySyncService.syncMeasurements()).thenReturn(450);

        mockMvc.perform(post("/admin/air-quality/sync/measurements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("GIOS_MEASUREMENTS"))
                .andExpect(jsonPath("$.recordsProcessed").value(450));
    }

    @Test
    void shouldTriggerIndexesSync() throws Exception {
        when(airQualitySyncService.syncAirQualityIndexes()).thenReturn(150);

        mockMvc.perform(post("/admin/air-quality/sync/indexes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("GIOS_INDEX"))
                .andExpect(jsonPath("$.recordsProcessed").value(150));
    }

    @Test
    void shouldTriggerForecastsSync() throws Exception {
        when(airQualitySyncService.syncForecasts()).thenReturn(400);

        mockMvc.perform(post("/admin/air-quality/sync/forecasts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("IOS_FORECAST"))
                .andExpect(jsonPath("$.recordsProcessed").value(400));
    }

    @Test
    void shouldTriggerAllSync() throws Exception {
        when(airQualitySyncService.syncAll()).thenReturn(Map.of("GIOS_STATIONS", 150));

        mockMvc.perform(post("/admin/air-quality/sync/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.results.GIOS_STATIONS").value(150));
    }

    @Test
    void shouldTriggerCleanup() throws Exception {
        when(cleanupService.cleanupOldData()).thenReturn(Map.of("status", "SUCCESS", "deletedMeasurements", 100));

        mockMvc.perform(post("/admin/air-quality/cleanup"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.deletedMeasurements").value(100));
    }

    @Test
    void shouldGetSyncLogs() throws Exception {
        AirQualitySyncLog log = AirQualitySyncLog.builder().id(1L).providerName("GIOS_STATIONS").status("SUCCESS").startedAt(LocalDateTime.now()).build();
        when(syncLogService.getLogs(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(log)));

        mockMvc.perform(get("/admin/air-quality/sync/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].providerName").value("GIOS_STATIONS"));
    }

    @Test
    void shouldToggleStationActive() throws Exception {
        when(stationManagementService.toggleStationActive(114L)).thenReturn(false);

        mockMvc.perform(patch("/admin/air-quality/stations/114/toggle-active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stationId").value(114))
                .andExpect(jsonPath("$.isActive").value(false));
    }

    @Test
    void shouldUpdateStationTeryt() throws Exception {
        GiosStation station = GiosStation.builder().id(114L).terytCode("1465").build();
        when(stationManagementService.updateStationTeryt(eq(114L), eq("1465"))).thenReturn(station);

        mockMvc.perform(patch("/admin/air-quality/stations/114/teryt")
                        .param("terytCode", "1465"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(114))
                .andExpect(jsonPath("$.terytCode").value("1465"));
    }
}
