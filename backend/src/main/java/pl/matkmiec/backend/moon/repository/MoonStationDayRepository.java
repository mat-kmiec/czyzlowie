package pl.matkmiec.backend.moon.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.matkmiec.backend.moon.model.MoonStationDay;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MoonStationDayRepository extends JpaRepository<MoonStationDay, Long> {

    Optional<MoonStationDay> findByStationIdAndCalculationDate(String stationId, LocalDate calculationDate);

    List<MoonStationDay> findByStationIdAndCalculationDateBetweenOrderByCalculationDateAsc(
            String stationId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT m FROM MoonStationDay m WHERE m.station.id = :stationId AND YEAR(m.calculationDate) = :year ORDER BY m.calculationDate ASC")
    List<MoonStationDay> findByStationIdAndYear(@Param("stationId") String stationId, @Param("year") int year);

    void deleteByCalculationDateBetween(LocalDate startDate, LocalDate endDate);

    @Query("SELECT COUNT(m) FROM MoonStationDay m WHERE YEAR(m.calculationDate) = :year")
    long countByYear(@Param("year") int year);
}
