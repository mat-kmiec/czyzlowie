package pl.matkmiec.backend.weather.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.matkmiec.backend.weather.model.ImgwHydroData;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ImgwHydroDataRepository extends JpaRepository<ImgwHydroData, Long> {
    List<ImgwHydroData> findAllByMeasurementTimeAfter(LocalDateTime time);
}
