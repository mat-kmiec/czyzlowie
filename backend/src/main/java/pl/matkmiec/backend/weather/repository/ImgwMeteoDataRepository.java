package pl.matkmiec.backend.weather.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.matkmiec.backend.weather.model.ImgwMeteoData;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ImgwMeteoDataRepository extends JpaRepository<ImgwMeteoData, Long> {
    List<ImgwMeteoData> findAllByMeasurementTimeAfter(LocalDateTime time);
}
