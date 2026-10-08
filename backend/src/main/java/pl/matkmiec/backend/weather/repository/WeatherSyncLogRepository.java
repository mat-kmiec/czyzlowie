package pl.matkmiec.backend.weather.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.matkmiec.backend.weather.model.WeatherSyncLog;

@Repository
public interface WeatherSyncLogRepository extends JpaRepository<WeatherSyncLog, Long> {}