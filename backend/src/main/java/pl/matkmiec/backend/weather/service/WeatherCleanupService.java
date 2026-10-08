package pl.matkmiec.backend.weather.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.matkmiec.backend.weather.config.WeatherProperties;
import pl.matkmiec.backend.weather.model.WeatherSyncLog;
import pl.matkmiec.backend.weather.repository.ImgwHydroDataRepository;
import pl.matkmiec.backend.weather.repository.ImgwMeteoDataRepository;
import pl.matkmiec.backend.weather.repository.ImgwSynopDataRepository;
import pl.matkmiec.backend.weather.repository.OpenMeteoForecastRepository;
import pl.matkmiec.backend.weather.repository.WeatherSyncLogRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
public class WeatherCleanupService {

    public static final String CLEANUP_PROVIDER_NAME = "WEATHER_CLEANUP";
    public static final String DEFAULT_ZONE = "Europe/Warsaw";
    public static final int DEFAULT_RETENTION_DAYS = 5;

    private final WeatherProperties weatherProperties;
    private final ImgwSynopDataRepository synopDataRepo;
    private final ImgwMeteoDataRepository meteoDataRepo;
    private final ImgwHydroDataRepository hydroDataRepo;
    private final OpenMeteoForecastRepository openMeteoForecastRepo;
    private final WeatherSyncLogRepository syncLogRepo;
    private final SyncLogService syncLogService;
    private final Clock clock;

    public WeatherCleanupService(
            WeatherProperties weatherProperties,
            ImgwSynopDataRepository synopDataRepo,
            ImgwMeteoDataRepository meteoDataRepo,
            ImgwHydroDataRepository hydroDataRepo,
            OpenMeteoForecastRepository openMeteoForecastRepo,
            WeatherSyncLogRepository syncLogRepo,
            SyncLogService syncLogService,
            @Autowired(required = false) Clock clock
    ) {
        this.weatherProperties = weatherProperties;
        this.synopDataRepo = synopDataRepo;
        this.meteoDataRepo = meteoDataRepo;
        this.hydroDataRepo = hydroDataRepo;
        this.openMeteoForecastRepo = openMeteoForecastRepo;
        this.syncLogRepo = syncLogRepo;
        this.syncLogService = syncLogService;
        this.clock = clock != null ? clock : Clock.systemDefaultZone();
    }

    @Scheduled(cron = "${weather.cleanup.cron:0 30 1 * * *}", zone = "${weather.cleanup.zone:Europe/Warsaw}")
    @Transactional
    public Map<String, Object> cleanupOldData() {
        int retentionDays = (weatherProperties.cleanup() != null && weatherProperties.cleanup().retentionDays() > 0)
                ? weatherProperties.cleanup().retentionDays()
                : DEFAULT_RETENTION_DAYS;
        return cleanupDataOlderThan(retentionDays);
    }

    @Transactional
    public Map<String, Object> cleanupDataOlderThan(int retentionDays) {
        if (weatherProperties.cleanup() != null && !weatherProperties.cleanup().enabled()) {
            log.info("Czyszczenie starych danych pogodowych jest wyłączone w konfiguracji.");
            return Map.of("status", "DISABLED", "message", "Cleanup is disabled in configuration");
        }

        LocalDateTime cutoffTime = calculateCutoffTime(retentionDays);
        log.info("Rozpoczynam automatyczne czyszczenie danych pogodowych starszych niż {} dni (przed: {})...", retentionDays, cutoffTime);
        WeatherSyncLog logEntry = syncLogService.logStart(CLEANUP_PROVIDER_NAME);

        try {
            int synopDeleted = synopDataRepo.deleteByMeasurementTimeBefore(cutoffTime);
            int meteoDeleted = meteoDataRepo.deleteByMeasurementTimeBefore(cutoffTime);
            int hydroDeleted = hydroDataRepo.deleteByMeasurementTimeBefore(cutoffTime);
            int openMeteoDeleted = openMeteoForecastRepo.deleteByForecastTimeBefore(cutoffTime);
            int syncLogsDeleted = syncLogRepo.deleteByStartedAtBefore(cutoffTime);

            int totalDeleted = synopDeleted + meteoDeleted + hydroDeleted + openMeteoDeleted + syncLogsDeleted;

            log.info("Pomyślnie wyczyszczono {} starych rekordów (Synop: {}, Meteo: {}, Hydro: {}, Open-Meteo: {}, Logi: {}).",
                    totalDeleted, synopDeleted, meteoDeleted, hydroDeleted, openMeteoDeleted, syncLogsDeleted);

            syncLogService.logSuccess(logEntry.getId(), totalDeleted);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "SUCCESS");
            result.put("cutoffTime", cutoffTime.toString());
            result.put("retentionDays", retentionDays);
            result.put("synopDeleted", synopDeleted);
            result.put("meteoDeleted", meteoDeleted);
            result.put("hydroDeleted", hydroDeleted);
            result.put("openMeteoDeleted", openMeteoDeleted);
            result.put("syncLogsDeleted", syncLogsDeleted);
            result.put("totalDeleted", totalDeleted);
            return result;
        } catch (Exception e) {
            log.error("Błąd podczas czyszczenia starych danych pogodowych: {}", e.getMessage(), e);
            syncLogService.logError(logEntry.getId(), e.getMessage());
            throw e;
        }
    }

    public LocalDateTime calculateCutoffTime(int daysToKeep) {
        String zoneName = (weatherProperties.cleanup() != null && weatherProperties.cleanup().zone() != null)
                ? weatherProperties.cleanup().zone()
                : DEFAULT_ZONE;
        ZoneId zoneId = ZoneId.of(zoneName);
        ZonedDateTime nowInZone = ZonedDateTime.now(clock.withZone(zoneId));
        return nowInZone.minusDays(daysToKeep).toLocalDateTime();
    }
}
