package pl.matkmiec.backend.weather.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pl.matkmiec.backend.weather.model.WeatherSyncLog;
import pl.matkmiec.backend.weather.repository.WeatherSyncLogRepository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncLogService {

    private final WeatherSyncLogRepository syncLogRepo;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public WeatherSyncLog logStart(String providerName) {
        WeatherSyncLog syncLog = WeatherSyncLog.builder()
                .providerName(providerName)
                .startedAt(LocalDateTime.now())
                .status("STARTED")
                .recordsProcessed(0)
                .build();
        return syncLogRepo.save(syncLog);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logSuccess(Long logId, int recordsProcessed) {
        if (logId == null) return;
        syncLogRepo.findById(logId).ifPresent(syncLog -> {
            syncLog.setStatus("SUCCESS");
            syncLog.setRecordsProcessed(recordsProcessed);
            syncLog.setFinishedAt(LocalDateTime.now());
            syncLogRepo.save(syncLog);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logError(Long logId, String errorMessage) {
        if (logId == null) return;
        syncLogRepo.findById(logId).ifPresent(syncLog -> {
            syncLog.setStatus("ERROR");
            syncLog.setErrorMessage(errorMessage);
            syncLog.setFinishedAt(LocalDateTime.now());
            syncLogRepo.save(syncLog);
        });
    }

    @Transactional(readOnly = true)
    public List<WeatherSyncLog> getRecentLogs() {
        return syncLogRepo.findAll();
    }
}
