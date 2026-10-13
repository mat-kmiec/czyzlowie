package pl.matkmiec.backend.fish.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.matkmiec.backend.fish.model.FishSpecies;
import pl.matkmiec.backend.fish.model.FishType;
import pl.matkmiec.backend.fish.model.LegalStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface FishSpeciesRepository extends JpaRepository<FishSpecies, Long> {

    Optional<FishSpecies> findBySlug(String slug);

    Optional<FishSpecies> findByNameIgnoreCase(String name);

    List<FishSpecies> findByIsActiveTrueOrderByDisplayOrderAscNameAsc();

    List<FishSpecies> findByFishTypeAndIsActiveTrueOrderByDisplayOrderAscNameAsc(FishType fishType);

    List<FishSpecies> findByLegalStatusAndIsActiveTrueOrderByDisplayOrderAscNameAsc(LegalStatus legalStatus);

    Long countByFishTypeAndIsActiveTrue(FishType fishType);

    @Query("SELECT f FROM FishSpecies f WHERE f.isActive = true AND (" +
            "LOWER(f.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(f.latinName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(f.family) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(f.shortDescription) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(f.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(f.similarSpecies) LIKE LOWER(CONCAT('%', :query, '%'))" +
            ") ORDER BY f.displayOrder ASC, f.name ASC")
    List<FishSpecies> searchActiveSpecies(@Param("query") String query);

    @Query("SELECT f FROM FishSpecies f WHERE f.isActive = true AND (" +
            "LOWER(f.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(f.latinName) LIKE LOWER(CONCAT('%', :query, '%'))" +
            ") ORDER BY f.displayOrder ASC, f.name ASC")
    List<FishSpecies> searchAutocomplete(@Param("query") String query, Pageable pageable);

    @Query("SELECT f FROM FishSpecies f WHERE f.isActive = true AND f.recordWeightKg IS NOT NULL ORDER BY f.recordWeightKg DESC")
    List<FishSpecies> findTopRecords();

    @Query(value = "SELECT * FROM fish_species WHERE is_active = true ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    Optional<FishSpecies> findRandomActiveSpecies();
}
