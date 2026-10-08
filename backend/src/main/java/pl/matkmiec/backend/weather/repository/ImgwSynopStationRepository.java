package pl.matkmiec.backend.weather.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.matkmiec.backend.weather.model.ImgwSynopStation;

import java.util.List;

@Repository
public interface ImgwSynopStationRepository extends JpaRepository<ImgwSynopStation, String> {
    List<ImgwSynopStation> findAllByIsActiveTrue();
}
