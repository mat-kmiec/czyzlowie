package pl.matkmiec.backend.airquality.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pl.matkmiec.backend.airquality.model.AirQualitySyncLog;
import pl.matkmiec.backend.airquality.repository.AirQualitySyncLogRepository;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AirQualitySyncLogService {

    private final AirQualitySyncLogRepository syncLogRepo;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AirQualitySyncLog logStart(String providerName) {
        AirQualitySyncLog syncLog = AirQualitySyncLog.builder()
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
    public Page<AirQualitySyncLog> getLogs(Pageable pageable) {
        return syncLogRepo.findAllByOrderByStartedAtDesc(pageable);
    }
}
