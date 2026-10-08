package pl.matkmiec.backend.weather.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.matkmiec.backend.weather.model.OpenMeteoForecast;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface OpenMeteoForecastRepository extends JpaRepository<OpenMeteoForecast, Long> {

    List<OpenMeteoForecast> findAllBySynopStation_IdInAndForecastTimeBetween(
            Collection<String> stationIds, LocalDateTime start, LocalDateTime end
    );

    List<OpenMeteoForecast> findAllByForecastTimeBetween(
            LocalDateTime start, LocalDateTime end
    );

    List<OpenMeteoForecast> findAllBySynopStation_IdOrderByForecastTimeAsc(
            String stationId
    );

    List<OpenMeteoForecast> findAllBySynopStation_IdAndForecastTimeAfterOrderByForecastTimeAsc(
            String stationId, LocalDateTime afterTime
    );

    List<OpenMeteoForecast> findAllBySynopStation_IdInAndForecastTimeAfterOrderByForecastTimeAsc(
            Collection<String> stationIds, LocalDateTime afterTime
    );

    Optional<OpenMeteoForecast> findBySynopStation_IdAndForecastTime(
            String stationId, LocalDateTime forecastTime
    );

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM OpenMeteoForecast f WHERE f.forecastTime < :cutoff")
    int deleteByForecastTimeBefore(@Param("cutoff") LocalDateTime cutoff);
}
