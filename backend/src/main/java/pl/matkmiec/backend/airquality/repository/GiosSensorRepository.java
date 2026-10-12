package pl.matkmiec.backend.airquality.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.matkmiec.backend.airquality.model.GiosSensor;

import java.util.List;

@Repository
public interface GiosSensorRepository extends JpaRepository<GiosSensor, Long> {

    List<GiosSensor> findAllByStationId(Long stationId);

    List<GiosSensor> findAllByStationIdAndIsActiveTrue(Long stationId);

    List<GiosSensor> findAllByIsActiveTrue();
}
