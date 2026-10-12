package pl.matkmiec.backend.airquality.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.matkmiec.backend.airquality.model.AirQualitySyncLog;

import java.time.LocalDateTime;

@Repository
public interface AirQualitySyncLogRepository extends JpaRepository<AirQualitySyncLog, Long> {

    Page<AirQualitySyncLog> findAllByOrderByStartedAtDesc(Pageable pageable);

    int deleteAllByStartedAtBefore(LocalDateTime beforeDate);
}
