package pl.matkmiec.backend.airquality.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.matkmiec.backend.airquality.model.IosAirQualityForecast;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface IosAirQualityForecastRepository extends JpaRepository<IosAirQualityForecast, Long> {

    List<IosAirQualityForecast> findAllByTerytCodeAndForecastDateGreaterThanEqualOrderByForecastDateAsc(String terytCode, LocalDate fromDate);

    List<IosAirQualityForecast> findAllByTerytCodeAndPollutantAndForecastDateGreaterThanEqualOrderByForecastDateAsc(String terytCode, String pollutant, LocalDate fromDate);

    Optional<IosAirQualityForecast> findByTerytCodeAndPollutantAndForecastDate(String terytCode, String pollutant, LocalDate forecastDate);

    @Modifying
    @Query("DELETE FROM IosAirQualityForecast f WHERE f.forecastDate < :beforeDate")
    int deleteAllByForecastDateBefore(@Param("beforeDate") LocalDate beforeDate);
}
