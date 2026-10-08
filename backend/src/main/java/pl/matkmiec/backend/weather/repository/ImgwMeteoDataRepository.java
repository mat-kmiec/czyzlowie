package pl.matkmiec.backend.weather.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.matkmiec.backend.weather.model.ImgwMeteoData;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ImgwMeteoDataRepository extends JpaRepository<ImgwMeteoData, Long> {
    List<ImgwMeteoData> findAllByMeasurementTimeAfter(LocalDateTime time);

    Page<ImgwMeteoData> findAllByStation_Id(String stationId, Pageable pageable);

    Page<ImgwMeteoData> findAllByStation_IdAndMeasurementTimeBetween(
            String stationId, LocalDateTime from, LocalDateTime to, Pageable pageable
    );

    Page<ImgwMeteoData> findAllByMeasurementTimeBetween(
            LocalDateTime from, LocalDateTime to, Pageable pageable
    );

    Optional<ImgwMeteoData> findFirstByStation_IdOrderByMeasurementTimeDesc(String stationId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM ImgwMeteoData d WHERE d.measurementTime < :cutoff")
    int deleteByMeasurementTimeBefore(@Param("cutoff") LocalDateTime cutoff);
}
