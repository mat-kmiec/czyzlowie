package pl.matkmiec.backend.airquality.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.matkmiec.backend.airquality.model.GiosMeasurement;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GiosMeasurementRepository extends JpaRepository<GiosMeasurement, Long> {

    List<GiosMeasurement> findAllByMeasurementTimeAfter(LocalDateTime afterTime);

    List<GiosMeasurement> findAllBySensorIdAndMeasurementTimeAfterOrderByMeasurementTimeAsc(Long sensorId, LocalDateTime afterTime);

    List<GiosMeasurement> findAllByStationIdAndMeasurementTimeAfterOrderByMeasurementTimeAsc(Long stationId, LocalDateTime afterTime);

    @Query("SELECT m FROM GiosMeasurement m WHERE m.stationId = :stationId AND (:paramCode IS NULL OR m.paramCode = :paramCode) AND (:from IS NULL OR m.measurementTime >= :from) AND (:to IS NULL OR m.measurementTime <= :to)")
    Page<GiosMeasurement> findHistory(
            @Param("stationId") Long stationId,
            @Param("paramCode") String paramCode,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );

    @Query("SELECT m FROM GiosMeasurement m WHERE m.paramCode = :paramCode AND m.measurementTime >= :since AND m.value IS NOT NULL ORDER BY m.measurementTime DESC")
    List<GiosMeasurement> findRecentByParamCode(@Param("paramCode") String paramCode, @Param("since") LocalDateTime since);

    @Query(value = """
        SELECT DISTINCT ON (m.station_id, m.param_code) m.*
        FROM gios_measurement m
        WHERE m.measurement_time >= :since AND m.value IS NOT NULL
        ORDER BY m.station_id, m.param_code, m.measurement_time DESC
    """, nativeQuery = true)
    List<GiosMeasurement> findLatestMeasurementsPerStationAndParam(@Param("since") LocalDateTime since);

    @Query("SELECT m FROM GiosMeasurement m WHERE m.stationId IN :stationIds AND m.measurementTime >= :since AND m.value IS NOT NULL ORDER BY m.measurementTime DESC")
    List<GiosMeasurement> findRecentByStationIds(@Param("stationIds") List<Long> stationIds, @Param("since") LocalDateTime since);

    @Modifying
    @Query("DELETE FROM GiosMeasurement m WHERE m.measurementTime < :beforeTime")
    int deleteAllByMeasurementTimeBefore(@Param("beforeTime") LocalDateTime beforeTime);
}
