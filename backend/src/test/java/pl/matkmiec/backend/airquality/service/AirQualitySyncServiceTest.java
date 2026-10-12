package pl.matkmiec.backend.airquality.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.matkmiec.backend.airquality.client.GiosApiClient;
import pl.matkmiec.backend.airquality.client.IosApiClient;
import pl.matkmiec.backend.airquality.config.AirQualityProperties;
import pl.matkmiec.backend.airquality.dto.*;
import pl.matkmiec.backend.airquality.mapper.GiosDataMapper;
import pl.matkmiec.backend.airquality.mapper.IosForecastMapper;
import pl.matkmiec.backend.airquality.model.*;
import pl.matkmiec.backend.airquality.repository.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AirQualitySyncServiceTest {

    @Mock
    private GiosApiClient giosApiClient;

    @Mock
    private IosApiClient iosApiClient;

    @Mock
    private GiosDataMapper giosMapper;

    @Mock
    private IosForecastMapper iosMapper;

    @Mock
    private AirQualityStationManagementService stationManagementService;

    @Mock
    private AirQualitySyncLogService syncLogService;

    @Mock
    private AirQualityProperties airQualityProperties;

    @Mock
    private GiosStationRepository stationRepo;

    @Mock
    private GiosSensorRepository sensorRepo;

    @Mock
    private GiosMeasurementRepository measurementRepo;

    @Mock
    private GiosAirQualityIndexRepository indexRepo;

    @Mock
    private IosAirQualityForecastRepository forecastRepo;

    @InjectMocks
    private AirQualitySyncService syncService;

    private AirQualityProperties.Sync syncConfig;

    @BeforeEach
    void setUp() {
        syncConfig = new AirQualityProperties.Sync(true, "0 0 3 * * *", "0 5 3 * * *", "0 10 * * * *", "0 12 * * * *", "0 0 */6 * * *", 50, 16, 48);
        lenient().when(airQualityProperties.sync()).thenReturn(syncConfig);
        lenient().when(syncLogService.logStart(anyString())).thenReturn(AirQualitySyncLog.builder().id(1L).build());
    }

    @Test
    void shouldSyncStations() {
        GiosStationResponseDto dto = new GiosStationResponseDto(114L, "Warszawa", 52.29, 21.03, null, null);
        when(giosApiClient.fetchAllStations()).thenReturn(List.of(dto));
        when(stationManagementService.autoDiscoverStations(anyList())).thenReturn(Map.of(114L, GiosStation.builder().id(114L).build()));

        int count = syncService.syncStations();

        assertEquals(1, count);
        verify(syncLogService, times(1)).logSuccess(eq(1L), eq(1));
    }

    @Test
    void shouldSyncSensors() {
        GiosStation station = GiosStation.builder().id(114L).isActive(true).build();
        GiosSensor sensor = GiosSensor.builder().id(650L).station(station).paramCode("PM10").build();
        GiosSensorResponseDto sensorDto = new GiosSensorResponseDto(650L, 114L, null);

        when(stationRepo.findAllByIsActiveTrue()).thenReturn(List.of(station));
        when(giosApiClient.fetchSensors(114L)).thenReturn(List.of(sensorDto));
        when(stationManagementService.autoDiscoverSensors(eq(station), anyList())).thenReturn(List.of(sensor));

        int count = syncService.syncSensors();

        assertEquals(1, count);
        verify(syncLogService, times(1)).logSuccess(eq(1L), eq(1));
    }

    @Test
    void shouldSyncMeasurements() {
        GiosStation station = GiosStation.builder().id(114L).build();
        GiosSensor sensor = GiosSensor.builder().id(650L).station(station).paramCode("PM10").build();
        GiosMeasurementValueDto valDto = new GiosMeasurementValueDto("2026-10-12 22:00:00", 30.5);
        GiosMeasurementDataResponseDto dataDto = new GiosMeasurementDataResponseDto("PM10", List.of(valDto));
        GiosMeasurement entity = GiosMeasurement.builder().sensor(sensor).stationId(114L).paramCode("PM10").measurementTime(LocalDateTime.now()).value(30.5).build();

        when(sensorRepo.findAllByIsActiveTrue()).thenReturn(List.of(sensor));
        when(giosApiClient.fetchMeasurementData(650L)).thenReturn(dataDto);
        when(giosMapper.toMeasurementEntity(any(), eq(sensor), eq("PM10"))).thenReturn(entity);
        when(measurementRepo.findAllByMeasurementTimeAfter(any(LocalDateTime.class))).thenReturn(Collections.emptyList());

        int count = syncService.syncMeasurements();

        assertEquals(1, count);
        verify(measurementRepo, times(1)).saveAll(anyList());
        verify(syncLogService, times(1)).logSuccess(eq(1L), eq(1));
    }

    @Test
    void shouldSyncAirQualityIndexes() {
        GiosStation station = GiosStation.builder().id(114L).isActive(true).build();
        GiosIndexResponseDto indexDto = new GiosIndexResponseDto(114L, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        GiosAirQualityIndex entity = GiosAirQualityIndex.builder().station(station).indexLevelId(1).build();

        when(stationRepo.findAllByIsActiveTrue()).thenReturn(List.of(station));
        when(giosApiClient.fetchAirQualityIndex(114L)).thenReturn(indexDto);
        when(indexRepo.findAll()).thenReturn(Collections.emptyList());
        when(giosMapper.toIndexEntity(eq(indexDto), eq(station))).thenReturn(entity);

        int count = syncService.syncAirQualityIndexes();

        assertEquals(1, count);
        verify(indexRepo, times(1)).saveAll(anyList());
        verify(syncLogService, times(1)).logSuccess(eq(1L), eq(1));
    }

    @Test
    void shouldSyncForecasts() {
        GiosStation station = GiosStation.builder().id(114L).terytCode("1465").isActive(true).build();
        IosAirQualityForecast forecast = IosAirQualityForecast.builder().terytCode("1465").pollutant("PM10").forecastDate(java.time.LocalDate.now().plusDays(1)).forecastValue(20.0).build();

        when(stationRepo.findAllByIsActiveTrue()).thenReturn(List.of(station));
        when(iosApiClient.fetchForecast(anyString(), eq("1465"))).thenReturn(Map.of("20261012", 20.0));
        when(iosMapper.toForecastEntities(eq("1465"), anyString(), anyMap())).thenReturn(List.of(forecast));
        when(forecastRepo.findAll()).thenReturn(Collections.emptyList());

        int count = syncService.syncForecasts();

        assertEquals(4, count); // 4 pollutants
        verify(forecastRepo, times(1)).saveAll(anyList());
        verify(syncLogService, times(1)).logSuccess(eq(1L), eq(4));
    }
}
