package pl.matkmiec.backend.fish.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import pl.matkmiec.backend.fish.dto.*;
import pl.matkmiec.backend.fish.exception.FishSpeciesNotFoundException;
import pl.matkmiec.backend.fish.mapper.FishMapper;
import pl.matkmiec.backend.fish.model.*;
import pl.matkmiec.backend.fish.repository.FishSpeciesRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FishQueryServiceTest {

    @Mock
    private FishSpeciesRepository fishSpeciesRepository;

    @Spy
    private FishMapper fishMapper = new FishMapper();

    @InjectMocks
    private FishQueryService fishQueryService;

    private FishSpecies szczupak;
    private FishSpecies karp;

    @BeforeEach
    void setUp() {
        szczupak = FishSpecies.builder()
                .id(1L)
                .slug("szczupak-pospolity")
                .name("Szczupak pospolity")
                .latinName("Esox lucius")
                .family("Szczupakowate (Esocidae)")
                .fishType(FishType.PREDATOR)
                .legalStatus(LegalStatus.NATIVE)
                .minSizeLimit(50)
                .protectionPeriod("1 stycznia - 30 kwietnia")
                .activeMonths("5,6,9,10,11,12")
                .recordWeightKg(24.10)
                .isActive(true)
                .build();

        karp = FishSpecies.builder()
                .id(2L)
                .slug("karp")
                .name("Karp")
                .latinName("Cyprinus carpio")
                .family("Karpiowate (Cyprinidae)")
                .fishType(FishType.PEACEFUL)
                .legalStatus(LegalStatus.NATIVE)
                .minSizeLimit(30)
                .protectionPeriod("Brak")
                .activeMonths("4,5,6,7,8,9,10")
                .recordWeightKg(34.50)
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Should return all species or filter by type/status/activeMonth")
    void shouldFilterSpeciesCorrectly() {
        when(fishSpeciesRepository.findByIsActiveTrueOrderByDisplayOrderAscNameAsc())
                .thenReturn(List.of(szczupak, karp));

        List<FishSpeciesListItemDto> all = fishQueryService.getSpeciesList(null, null, null, null, LocalDate.now());
        assertEquals(2, all.size());

        when(fishSpeciesRepository.findByFishTypeAndIsActiveTrueOrderByDisplayOrderAscNameAsc(FishType.PREDATOR))
                .thenReturn(List.of(szczupak));

        List<FishSpeciesListItemDto> predators = fishQueryService.getSpeciesList(FishType.PREDATOR, null, null, null, LocalDate.now());
        assertEquals(1, predators.size());
        assertEquals("Szczupak pospolity", predators.get(0).getName());
    }

    @Test
    @DisplayName("Should fetch species details by numeric ID and by slug string")
    void shouldFetchSpeciesByIdOrSlug() {
        when(fishSpeciesRepository.findById(1L)).thenReturn(Optional.of(szczupak));
        when(fishSpeciesRepository.findBySlug("karp")).thenReturn(Optional.of(karp));

        FishSpeciesDetailsDto byId = fishQueryService.getSpeciesByIdOrSlug("1", LocalDate.now());
        assertNotNull(byId);
        assertEquals("Szczupak pospolity", byId.getName());

        FishSpeciesDetailsDto bySlug = fishQueryService.getSpeciesByIdOrSlug("karp", LocalDate.now());
        assertNotNull(bySlug);
        assertEquals("Karp", bySlug.getName());

        when(fishSpeciesRepository.findBySlug("nieznana-ryba")).thenReturn(Optional.empty());
        assertThrows(FishSpeciesNotFoundException.class, () -> fishQueryService.getSpeciesByIdOrSlug("nieznana-ryba", LocalDate.now()));
    }

    @Test
    @DisplayName("Should build categories summary with species counts")
    void shouldBuildCategoriesSummary() {
        when(fishSpeciesRepository.findByIsActiveTrueOrderByDisplayOrderAscNameAsc())
                .thenReturn(List.of(szczupak, karp));

        List<FishCategorySummaryDto> categories = fishQueryService.getCategoriesSummary(LocalDate.now());

        assertNotNull(categories);
        assertEquals(FishType.values().length, categories.size());

        FishCategorySummaryDto predatorCat = categories.stream()
                .filter(c -> c.getFishType() == FishType.PREDATOR)
                .findFirst()
                .orElseThrow();
        assertEquals(1L, predatorCat.getSpeciesCount());
        assertEquals(1, predatorCat.getTopSpecies().size());
    }

    @Test
    @DisplayName("Should build monthly fishing calendar")
    void shouldBuildMonthlyFishingCalendar() {
        when(fishSpeciesRepository.findByIsActiveTrueOrderByDisplayOrderAscNameAsc())
                .thenReturn(List.of(szczupak, karp));

        // Month 5 (May): both active, none protected (szczupak protection ends in April)
        FishCalendarMonthDto mayCalendar = fishQueryService.getCalendarMonth(5, LocalDate.of(2026, 5, 1));
        assertNotNull(mayCalendar);
        assertEquals(5, mayCalendar.getMonthNumber());
        assertEquals("Maj", mayCalendar.getMonthName());
        assertEquals(2, mayCalendar.getActiveSpecies().size());
        assertEquals(0, mayCalendar.getProtectedSpecies().size());

        // Month 3 (March): szczupak in protection period
        FishCalendarMonthDto marchCalendar = fishQueryService.getCalendarMonth(3, LocalDate.of(2026, 3, 1));
        assertEquals(3, marchCalendar.getMonthNumber());
        assertEquals(1, marchCalendar.getProtectedSpecies().size());
        assertEquals("Szczupak pospolity", marchCalendar.getProtectedSpecies().get(0).getName());
    }

    @Test
    @DisplayName("Should return records leaderboard sorted by record weight")
    void shouldReturnRecordsLeaderboard() {
        when(fishSpeciesRepository.findTopRecords()).thenReturn(List.of(karp, szczupak));

        List<FishRecordLeaderboardDto> records = fishQueryService.getRecordLeaderboard();
        assertNotNull(records);
        assertEquals(2, records.size());
        assertEquals(34.50, records.get(0).getRecordWeightKg());
        assertEquals(24.10, records.get(1).getRecordWeightKg());
    }

    @Test
    @DisplayName("Should return search autocomplete suggestions")
    void shouldReturnAutocompleteSuggestions() {
        when(fishSpeciesRepository.searchAutocomplete(eq("szcz"), any(Pageable.class)))
                .thenReturn(List.of(szczupak));

        List<FishSearchSuggestionDto> suggestions = fishQueryService.searchAutocomplete("szcz", 5);
        assertNotNull(suggestions);
        assertEquals(1, suggestions.size());
        assertEquals("Szczupak pospolity", suggestions.get(0).getName());
    }

    @Test
    @DisplayName("Should return random active species")
    void shouldReturnRandomSpecies() {
        when(fishSpeciesRepository.findRandomActiveSpecies()).thenReturn(Optional.of(szczupak));

        FishSpeciesDetailsDto random = fishQueryService.getRandomSpecies(LocalDate.now());
        assertNotNull(random);
        assertEquals("Szczupak pospolity", random.getName());
    }

    @Test
    @DisplayName("Should return species tackle guide and C&R guide")
    void shouldReturnTackleAndCatchAndReleaseGuides() {
        szczupak.setLineMonofilament("0.28 - 0.35 mm");
        szczupak.setRecommendedCastingWeight("15 - 50g");
        szczupak.setLeaderRequired(true);
        szczupak.setLandingMethod("Podbierak z siatką gumowaną");
        szczupak.setSafetyAlerts("Ostre zęby");
        szczupak.setBarblessHooksRecommended(true);

        when(fishSpeciesRepository.findBySlug("szczupak-pospolity")).thenReturn(Optional.of(szczupak));

        FishTackleGuideDto tackle = fishQueryService.getTackleGuide("szczupak-pospolity");
        assertNotNull(tackle);
        assertEquals("0.28 - 0.35 mm", tackle.getLineMonofilament());
        assertTrue(tackle.getLeaderRequired());

        FishCatchAndReleaseDto cr = fishQueryService.getCatchAndReleaseGuide("szczupak-pospolity");
        assertNotNull(cr);
        assertEquals("Podbierak z siatką gumowaną", cr.getLandingMethod());
        assertTrue(cr.getHasSafetyAlerts());
        assertTrue(cr.getBarblessHooksRecommended());
    }

    @Test
    @DisplayName("Should return all tackle guides and safety alerts catalog")
    void shouldReturnAllTackleGuidesAndSafetyAlerts() {
        szczupak.setSafetyAlerts("Ostre zęby");
        when(fishSpeciesRepository.findByIsActiveTrueOrderByDisplayOrderAscNameAsc())
                .thenReturn(List.of(szczupak, karp));

        List<FishTackleSummaryDto> tackleList = fishQueryService.getAllTackleGuides(null);
        assertEquals(2, tackleList.size());

        List<FishSafetyAlertDto> safetyAlerts = fishQueryService.getSafetyAlerts();
        assertEquals(1, safetyAlerts.size());
        assertEquals("szczupak-pospolity", safetyAlerts.getFirst().getSlug());
    }
}
