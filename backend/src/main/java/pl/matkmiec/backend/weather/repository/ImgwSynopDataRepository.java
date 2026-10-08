package pl.matkmiec.backend.weather.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.matkmiec.backend.weather.model.ImgwSynopData;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ImgwSynopDataRepository extends JpaRepository<ImgwSynopData, Long> {
    List<ImgwSynopData> findAllByMeasurementTimeAfter(LocalDateTime time);

    Page<ImgwSynopData> findAllByStation_Id(String stationId, Pageable pageable);

    Page<ImgwSynopData> findAllByStation_IdAndMeasurementTimeBetween(
            String stationId, LocalDateTime from, LocalDateTime to, Pageable pageable
    );

    Page<ImgwSynopData> findAllByMeasurementTimeBetween(
            LocalDateTime from, LocalDateTime to, Pageable pageable
    );

    Optional<ImgwSynopData> findFirstByStation_IdOrderByMeasurementTimeDesc(String stationId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM ImgwSynopData d WHERE d.measurementTime < :cutoff")
    int deleteByMeasurementTimeBefore(@Param("cutoff") LocalDateTime cutoff);
}
