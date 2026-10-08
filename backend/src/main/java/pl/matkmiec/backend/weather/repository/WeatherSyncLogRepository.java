package pl.matkmiec.backend.weather.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.matkmiec.backend.weather.model.WeatherSyncLog;

import java.time.LocalDateTime;

@Repository
public interface WeatherSyncLogRepository extends JpaRepository<WeatherSyncLog, Long> {

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM WeatherSyncLog l WHERE l.startedAt < :cutoff")
    int deleteByStartedAtBefore(@Param("cutoff") LocalDateTime cutoff);
}