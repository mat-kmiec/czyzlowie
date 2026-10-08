package pl.matkmiec.backend.weather.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.matkmiec.backend.weather.model.ImgwHydroStation;

import java.util.List;

@Repository
public interface ImgwHydroStationRepository extends JpaRepository<ImgwHydroStation, String> {
    List<ImgwHydroStation> findAllByIsActiveTrue();
}
