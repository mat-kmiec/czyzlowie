package pl.matkmiec.backend.weather.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.matkmiec.backend.weather.model.ImgwMeteoStation;

import java.util.List;

@Repository
public interface ImgwMeteoStationRepository extends JpaRepository<ImgwMeteoStation, String> {
    List<ImgwMeteoStation> findAllByIsActiveTrue();
}
