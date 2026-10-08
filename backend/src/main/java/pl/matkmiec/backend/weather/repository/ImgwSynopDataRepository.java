package pl.matkmiec.backend.weather.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.matkmiec.backend.weather.model.ImgwSynopData;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ImgwSynopDataRepository extends JpaRepository<ImgwSynopData, Long> {
    List<ImgwSynopData> findAllByMeasurementTimeAfter(LocalDateTime time);
}
