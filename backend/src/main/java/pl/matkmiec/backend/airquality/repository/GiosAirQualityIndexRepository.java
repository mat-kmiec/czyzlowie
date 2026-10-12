package pl.matkmiec.backend.airquality.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import pl.matkmiec.backend.airquality.model.GiosAirQualityIndex;

import java.util.List;
import java.util.Optional;

@Repository
public interface GiosAirQualityIndexRepository extends JpaRepository<GiosAirQualityIndex, Long> {

    Optional<GiosAirQualityIndex> findByStationId(Long stationId);

    List<GiosAirQualityIndex> findAllByStationIdIn(List<Long> stationIds);

    @Query("SELECT i FROM GiosAirQualityIndex i JOIN FETCH i.station s WHERE s.isActive = true")
    List<GiosAirQualityIndex> findAllActiveWithStation();
}
