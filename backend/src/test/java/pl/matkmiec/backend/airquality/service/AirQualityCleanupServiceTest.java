package pl.matkmiec.backend.airquality.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.matkmiec.backend.airquality.config.AirQualityProperties;
import pl.matkmiec.backend.airquality.repository.AirQualitySyncLogRepository;
import pl.matkmiec.backend.airquality.repository.GiosMeasurementRepository;
import pl.matkmiec.backend.airquality.repository.IosAirQualityForecastRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AirQualityCleanupServiceTest {

    @Mock
    private GiosMeasurementRepository measurementRepo;

    @Mock
    private IosAirQualityForecastRepository forecastRepo;

    @Mock
    private AirQualitySyncLogRepository syncLogRepo;

    @Mock
    private AirQualityProperties airQualityProperties;

    @InjectMocks
    private AirQualityCleanupService cleanupService;

    @BeforeEach
    void setUp() {
        AirQualityProperties.Cleanup cleanup = new AirQualityProperties.Cleanup(true, "0 45 1 * * *", "Europe/Warsaw", 7);
        lenient().when(airQualityProperties.cleanup()).thenReturn(cleanup);
    }

    @Test
    void shouldCleanupDataOlderThan() {
        when(measurementRepo.deleteAllByMeasurementTimeBefore(any(LocalDateTime.class))).thenReturn(150);
        when(forecastRepo.deleteAllByForecastDateBefore(any(LocalDate.class))).thenReturn(20);
        when(syncLogRepo.deleteAllByStartedAtBefore(any(LocalDateTime.class))).thenReturn(5);

        Map<String, Object> result = cleanupService.cleanupDataOlderThan(7);

        assertEquals("SUCCESS", result.get("status"));
        assertEquals(7, result.get("retentionDays"));
        assertEquals(150, result.get("deletedMeasurements"));
        assertEquals(20, result.get("deletedForecasts"));
        assertEquals(5, result.get("deletedLogs"));
    }
}
