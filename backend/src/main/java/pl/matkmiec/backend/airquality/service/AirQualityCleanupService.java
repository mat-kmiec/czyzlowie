package pl.matkmiec.backend.airquality.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.matkmiec.backend.airquality.config.AirQualityProperties;
import pl.matkmiec.backend.airquality.repository.AirQualitySyncLogRepository;
import pl.matkmiec.backend.airquality.repository.GiosMeasurementRepository;
import pl.matkmiec.backend.airquality.repository.IosAirQualityForecastRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AirQualityCleanupService {

    private final GiosMeasurementRepository measurementRepo;
    private final IosAirQualityForecastRepository forecastRepo;
    private final AirQualitySyncLogRepository syncLogRepo;
    private final AirQualityProperties airQualityProperties;

    @Scheduled(cron = "${airquality.cleanup.cron:0 45 1 * * *}", zone = "${airquality.cleanup.zone:Europe/Warsaw}")
    @Transactional
    public Map<String, Object> cleanupOldData() {
        if (!airQualityProperties.cleanup().enabled()) {
            log.info("Czyszczenie starych danych jakości powietrza jest wyłączone w konfiguracji.");
            return Map.of("status", "DISABLED");
        }
        return cleanupDataOlderThan(airQualityProperties.cleanup().retentionDays());
    }

    @Transactional
    public Map<String, Object> cleanupDataOlderThan(int retentionDays) {
        log.info("Rozpoczynam usuwanie danych jakości powietrza starszych niż {} dni...", retentionDays);
        LocalDateTime thresholdDateTime = LocalDateTime.now().minusDays(retentionDays);
        LocalDate thresholdDate = LocalDate.now().minusDays(retentionDays);

        int deletedMeasurements = measurementRepo.deleteAllByMeasurementTimeBefore(thresholdDateTime);
        int deletedForecasts = forecastRepo.deleteAllByForecastDateBefore(thresholdDate);
        int deletedLogs = syncLogRepo.deleteAllByStartedAtBefore(thresholdDateTime.minusDays(retentionDays));

        log.info("Zakończono czyszczenie: usunięto {} pomiarów, {} prognoz, {} logów.",
                deletedMeasurements, deletedForecasts, deletedLogs);

        Map<String, Object> result = new HashMap<>();
        result.put("status", "SUCCESS");
        result.put("retentionDays", retentionDays);
        result.put("deletedMeasurements", deletedMeasurements);
        result.put("deletedForecasts", deletedForecasts);
        result.put("deletedLogs", deletedLogs);
        return result;
    }
}
