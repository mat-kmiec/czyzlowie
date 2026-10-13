package pl.matkmiec.backend.moon.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.matkmiec.backend.moon.model.MoonGeneralDay;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MoonGeneralDayRepository extends JpaRepository<MoonGeneralDay, LocalDate> {

    Optional<MoonGeneralDay> findByCalculationDate(LocalDate calculationDate);

    List<MoonGeneralDay> findByCalculationDateBetweenOrderByCalculationDateAsc(LocalDate startDate, LocalDate endDate);

    @Query("SELECT m FROM MoonGeneralDay m WHERE YEAR(m.calculationDate) = :year ORDER BY m.calculationDate ASC")
    List<MoonGeneralDay> findByYear(@Param("year") int year);
}
