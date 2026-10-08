package pl.matkmiec.backend.weather.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.matkmiec.backend.weather.config.WeatherProperties;
import pl.matkmiec.backend.weather.model.WeatherSyncLog;
import pl.matkmiec.backend.weather.repository.*;

import java.time.*;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeatherCleanupServiceTest {

    @Mock
    private ImgwSynopDataRepository synopDataRepo;

    @Mock
    private ImgwMeteoDataRepository meteoDataRepo;

    @Mock
    private ImgwHydroDataRepository hydroDataRepo;

    @Mock
    private OpenMeteoForecastRepository openMeteoForecastRepo;

    @Mock
    private WeatherSyncLogRepository syncLogRepo;

    @Mock
    private SyncLogService syncLogService;

    private WeatherProperties weatherProperties;

    @BeforeEach
    void setUp() {
        weatherProperties = new WeatherProperties(
                new WeatherProperties.Api("https://danepubliczne.imgw.pl/api", "https://api.open-meteo.com/v1"),
                new WeatherProperties.Sync(true, "0 15 * * * *", "0 */10 * * * *", "0 */10 * * * *", "0 0 */6 * * *", 1, 6, 50, 48),
                new WeatherProperties.Cleanup(true, "0 30 1 * * *", "Europe/Warsaw", 5)
        );
    }

    @Test
    void shouldCalculateCutoffTimeCorrectlyForStandardDate() {
        // Fixed time: 2026-10-08 01:30:00 Europe/Warsaw
        ZonedDateTime fixedNow = ZonedDateTime.of(2026, 10, 8, 1, 30, 0, 0, ZoneId.of("Europe/Warsaw"));
        Clock fixedClock = Clock.fixed(fixedNow.toInstant(), ZoneId.of("Europe/Warsaw"));

        WeatherCleanupService service = new WeatherCleanupService(
                weatherProperties, synopDataRepo, meteoDataRepo, hydroDataRepo,
                openMeteoForecastRepo, syncLogRepo, syncLogService, fixedClock
        );

        LocalDateTime cutoff = service.calculateCutoffTime(5);

        // 5 days before 2026-10-08 01:30:00 is 2026-10-03 01:30:00
        assertThat(cutoff).isEqualTo(LocalDateTime.of(2026, 10, 3, 1, 30, 0));
    }

    @Test
    void shouldHandleWinterToSummerDstTransitionInPoland() {
        // In Poland, transition from CET (UTC+1) to CEST (UTC+2) occurs on the last Sunday of March.
        // In 2026, that is 2026-03-29 (clock moves 02:00 -> 03:00).
        // 5 days after transition: 2026-04-01 01:30 CEST.
        ZonedDateTime afterDst = ZonedDateTime.of(2026, 4, 1, 1, 30, 0, 0, ZoneId.of("Europe/Warsaw"));
        Clock clock = Clock.fixed(afterDst.toInstant(), ZoneId.of("Europe/Warsaw"));

        WeatherCleanupService service = new WeatherCleanupService(
                weatherProperties, synopDataRepo, meteoDataRepo, hydroDataRepo,
                openMeteoForecastRepo, syncLogRepo, syncLogService, clock
        );

        LocalDateTime cutoff = service.calculateCutoffTime(5);

        // 5 calendar days before 2026-04-01 01:30 is 2026-03-27 01:30
        assertThat(cutoff).isEqualTo(LocalDateTime.of(2026, 3, 27, 1, 30, 0));
    }

    @Test
    void shouldHandleSummerToWinterDstTransitionInPoland() {
        // In Poland, transition from CEST (UTC+2) to CET (UTC+1) occurs on the last Sunday of October.
        // In 2026, that is 2026-10-25 (clock moves 03:00 -> 02:00).
        // On 2026-10-28 01:30 CET (3 days after transition).
        ZonedDateTime afterDst = ZonedDateTime.of(2026, 10, 28, 1, 30, 0, 0, ZoneId.of("Europe/Warsaw"));
        Clock clock = Clock.fixed(afterDst.toInstant(), ZoneId.of("Europe/Warsaw"));

        WeatherCleanupService service = new WeatherCleanupService(
                weatherProperties, synopDataRepo, meteoDataRepo, hydroDataRepo,
                openMeteoForecastRepo, syncLogRepo, syncLogService, clock
        );

        LocalDateTime cutoff = service.calculateCutoffTime(5);

        // 5 calendar days before 2026-10-28 01:30 is 2026-10-23 01:30
        assertThat(cutoff).isEqualTo(LocalDateTime.of(2026, 10, 23, 1, 30, 0));
    }

    @Test
    void shouldCleanOldDataAndReturnSummary() {
        ZonedDateTime fixedNow = ZonedDateTime.of(2026, 10, 8, 1, 30, 0, 0, ZoneId.of("Europe/Warsaw"));
        Clock fixedClock = Clock.fixed(fixedNow.toInstant(), ZoneId.of("Europe/Warsaw"));

        WeatherCleanupService service = new WeatherCleanupService(
                weatherProperties, synopDataRepo, meteoDataRepo, hydroDataRepo,
                openMeteoForecastRepo, syncLogRepo, syncLogService, fixedClock
        );

        WeatherSyncLog mockLog = WeatherSyncLog.builder().id(999L).providerName("WEATHER_CLEANUP").build();
        when(syncLogService.logStart("WEATHER_CLEANUP")).thenReturn(mockLog);

        LocalDateTime expectedCutoff = LocalDateTime.of(2026, 10, 3, 1, 30, 0);
        when(synopDataRepo.deleteByMeasurementTimeBefore(expectedCutoff)).thenReturn(10);
        when(meteoDataRepo.deleteByMeasurementTimeBefore(expectedCutoff)).thenReturn(20);
        when(hydroDataRepo.deleteByMeasurementTimeBefore(expectedCutoff)).thenReturn(30);
        when(openMeteoForecastRepo.deleteByForecastTimeBefore(expectedCutoff)).thenReturn(40);
        when(syncLogRepo.deleteByStartedAtBefore(expectedCutoff)).thenReturn(5);

        Map<String, Object> result = service.cleanupOldData();

        assertThat(result.get("status")).isEqualTo("SUCCESS");
        assertThat(result.get("retentionDays")).isEqualTo(5);
        assertThat(result.get("synopDeleted")).isEqualTo(10);
        assertThat(result.get("meteoDeleted")).isEqualTo(20);
        assertThat(result.get("hydroDeleted")).isEqualTo(30);
        assertThat(result.get("openMeteoDeleted")).isEqualTo(40);
        assertThat(result.get("syncLogsDeleted")).isEqualTo(5);
        assertThat(result.get("totalDeleted")).isEqualTo(105);

        verify(syncLogService).logSuccess(999L, 105);
    }

    @Test
    void shouldNotCleanDataWhenDisabled() {
        WeatherProperties disabledProps = new WeatherProperties(
                weatherProperties.api(),
                weatherProperties.sync(),
                new WeatherProperties.Cleanup(false, "0 30 1 * * *", "Europe/Warsaw", 5)
        );

        WeatherCleanupService service = new WeatherCleanupService(
                disabledProps, synopDataRepo, meteoDataRepo, hydroDataRepo,
                openMeteoForecastRepo, syncLogRepo, syncLogService, Clock.systemDefaultZone()
        );

        Map<String, Object> result = service.cleanupOldData();

        assertThat(result.get("status")).isEqualTo("DISABLED");
        verifyNoInteractions(synopDataRepo, meteoDataRepo, hydroDataRepo, openMeteoForecastRepo, syncLogRepo, syncLogService);
    }
}
