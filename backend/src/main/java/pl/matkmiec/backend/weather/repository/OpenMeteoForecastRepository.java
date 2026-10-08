package pl.matkmiec.backend.weather.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.matkmiec.backend.weather.model.OpenMeteoForecast;

@Repository
public interface OpenMeteoForecastRepository extends JpaRepository<OpenMeteoForecast, Long> {}
