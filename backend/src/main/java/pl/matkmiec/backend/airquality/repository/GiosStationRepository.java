package pl.matkmiec.backend.airquality.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import pl.matkmiec.backend.airquality.model.GiosStation;

import java.util.List;
import java.util.Optional;

@Repository
public interface GiosStationRepository extends JpaRepository<GiosStation, Long> {

    List<GiosStation> findAllByIsActiveTrue();

    Optional<GiosStation> findByIdAndIsActiveTrue(Long id);

    @Query("SELECT s FROM GiosStation s WHERE s.isActive = true AND s.gegrLat IS NOT NULL AND s.gegrLon IS NOT NULL")
    List<GiosStation> findAllActiveWithCoordinates();

    List<GiosStation> findAllByTerytCode(String terytCode);

    List<GiosStation> findAllByCityNameIgnoreCase(String cityName);
}
