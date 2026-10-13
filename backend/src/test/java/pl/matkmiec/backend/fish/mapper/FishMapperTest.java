package pl.matkmiec.backend.fish.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.matkmiec.backend.fish.dto.*;
import pl.matkmiec.backend.fish.model.*;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FishMapperTest {

    private FishMapper fishMapper;

    @BeforeEach
    void setUp() {
        fishMapper = new FishMapper();
    }

    @Test
    @DisplayName("Should correctly map FishSpecies entity to FishSpeciesListItemDto and details")
    void shouldMapEntityToListItemAndDetailsDto() {
        FishSpecies species = FishSpecies.builder()
                .id(1L)
                .slug("szczupak-pospolity")
                .name("Szczupak pospolity")
                .latinName("Esox lucius")
                .family("Szczupakowate (Esocidae)")
                .fishType(FishType.PREDATOR)
                .imageUrl("http://example.com/szczupak.jpg")
                .thumbnailUrl("http://example.com/szczupak_thumb.jpg")
                .shortDescription("Drapieżnik słodkowodny")
                .description("Pełny opis szczupaka pospolitego.")
                .identificationFeatures("Kaczy dziób, ostre zęby")
                .minSizeLimit(50)
                .maxSizeLimit(90)
                .protectionPeriod("1 stycznia - 30 kwietnia")
                .dailyLimit("2 szt.")
                .legalStatus(LegalStatus.NATIVE)
                .legalNotes("Zakaz trollingu na wybranych akwenach")
                .recordWeightKg(24.10)
                .recordLengthCm(128.0)
                .recordYear(1976)
                .recordAngler("Wacław Biegan")
                .averageWeight("1.5 - 5.0 kg")
                .averageLength("50 - 80 cm")
                .waterPreferences("Jeziora, rzeki nizinne, zaporówki")
                .waterZone(WaterZone.LITTORAL)
                .naturalFood("Ryby, żaby, raki")
                .spawningMonths("Marzec - Kwiecień")
                .activeMonths("5,6,9,10,11,12")
                .similarSpecies("Brak")
                .dailyActivity("Świt i zmierzch")
                .seasonalActivity("Maj oraz jesień")
                .weatherPreferences("Pochmurno, wiatr")
                .effectiveMethods("Spinning, Trolling, Żywiec")
                .bestArtificialBaits("Woblery jerkbait, gumy")
                .bestNaturalBaits("Płoć, ukleja")
                .biteAndFightCharacteristics("Twarde uderzenie, świece")
                .typicalSpots("Trzcinowiska, grążele")
                .difficulty(DifficultyLevel.EASY)
                .lineMonofilament("0.28 - 0.35 mm")
                .lineBraid("0.14 - 0.20 mm")
                .recommendedHookSize("#2/0 - #4/0")
                .recommendedCastingWeight("15 - 50g")
                .recommendedRodAction("Fast 2.40m")
                .leaderRequired(true)
                .recommendedLeader("Przypon wolframowy/tytanowy min. 30 cm")
                .additionalGearTips("Mocny kołowrotek 3000-4000")
                .landingMethod("Podbierak z siatką gumowaną")
                .requiredGearCAndR("Długie szczypce, rozwieracz do paszczy")
                .safetyAlerts("Ostre zęby – ryzyko ran ciętych")
                .specialCareTips("Dotykać tylko mokrymi dłońmi")
                .barblessHooksRecommended(true)
                .displayOrder(1)
                .isActive(true)
                .build();

        // Reference date: 15 marca 2026 (during protection period)
        LocalDate inProtectionDate = LocalDate.of(2026, 3, 15);
        FishSpeciesListItemDto itemDto = fishMapper.toListItemDto(species, inProtectionDate);

        assertNotNull(itemDto);
        assertEquals(1L, itemDto.getId());
        assertEquals("Szczupak pospolity", itemDto.getName());
        assertEquals(FishType.PREDATOR, itemDto.getFishType());
        assertEquals("Drapieżnik", itemDto.getFishTypeDisplayName());
        assertEquals(50, itemDto.getMinSizeLimit());
        assertEquals(90, itemDto.getMaxSizeLimit());
        assertTrue(itemDto.getIsProtectedNow());
        assertEquals(List.of(5, 6, 9, 10, 11, 12), itemDto.getActiveMonths());

        // Reference date: 15 maja 2026 (outside protection period)
        LocalDate outProtectionDate = LocalDate.of(2026, 5, 15);
        FishSpeciesDetailsDto detailsDto = fishMapper.toDetailsDto(species, outProtectionDate);

        assertNotNull(detailsDto);
        assertEquals("Esox lucius", detailsDto.getLatinName());
        assertFalse(detailsDto.getIsProtectedNow());

        // Check section I
        assertNotNull(detailsDto.getBasicInfo());
        assertEquals("Szczupakowate (Esocidae)", detailsDto.getBasicInfo().getFamily());
        assertEquals("Kaczy dziób, ostre zęby", detailsDto.getBasicInfo().getIdentificationFeatures());

        // Check section II
        assertNotNull(detailsDto.getLegalInfo());
        assertEquals("1 stycznia - 30 kwietnia", detailsDto.getLegalInfo().getProtectionPeriod());
        assertEquals("2 szt.", detailsDto.getLegalInfo().getDailyLimit());

        // Check section III
        assertNotNull(detailsDto.getBiologyAndRecords());
        assertEquals(24.10, detailsDto.getBiologyAndRecords().getRecord().getWeightKg());
        assertEquals("Wacław Biegan", detailsDto.getBiologyAndRecords().getRecord().getAngler());
        assertEquals(WaterZone.LITTORAL, detailsDto.getBiologyAndRecords().getWaterZone());
        assertTrue(detailsDto.getBiologyAndRecords().getWaterPreferences().contains("Jeziora"));

        // Check section IV
        assertNotNull(detailsDto.getTacticsAndTips());
        assertEquals(DifficultyLevel.EASY, detailsDto.getTacticsAndTips().getDifficulty());
        assertTrue(detailsDto.getTacticsAndTips().getEffectiveMethods().contains("Spinning"));
        assertTrue(detailsDto.getTacticsAndTips().getEffectiveMethods().contains("Trolling"));

        // Check section V (Tackle Guide)
        assertNotNull(detailsDto.getTackleGuide());
        assertEquals("0.28 - 0.35 mm", detailsDto.getTackleGuide().getLineMonofilament());
        assertEquals("0.14 - 0.20 mm", detailsDto.getTackleGuide().getLineBraid());
        assertTrue(detailsDto.getTackleGuide().getLeaderRequired());
        assertEquals("15 - 50g", detailsDto.getTackleGuide().getRecommendedCastingWeight());

        // Check section VI (Catch & Release & Safety)
        assertNotNull(detailsDto.getCatchAndReleaseGuide());
        assertEquals("Podbierak z siatką gumowaną", detailsDto.getCatchAndReleaseGuide().getLandingMethod());
        assertTrue(detailsDto.getCatchAndReleaseGuide().getHasSafetyAlerts());
        assertTrue(detailsDto.getCatchAndReleaseGuide().getSafetyAlerts().getFirst().contains("Ostre zęby"));
        assertTrue(detailsDto.getCatchAndReleaseGuide().getRequiredGear().contains("Długie szczypce"));
        assertTrue(detailsDto.getCatchAndReleaseGuide().getBarblessHooksRecommended());
    }

    @Test
    @DisplayName("Should detect protection period accurately across different dates and formats")
    void shouldDetectProtectionPeriodCorrectly() {
        FishSpecies szczupak = FishSpecies.builder()
                .legalStatus(LegalStatus.NATIVE)
                .protectionPeriod("1 stycznia - 30 kwietnia")
                .build();

        assertTrue(fishMapper.isProtectedOnDate(szczupak, LocalDate.of(2026, 1, 1)));
        assertTrue(fishMapper.isProtectedOnDate(szczupak, LocalDate.of(2026, 3, 20)));
        assertTrue(fishMapper.isProtectedOnDate(szczupak, LocalDate.of(2026, 4, 30)));
        assertFalse(fishMapper.isProtectedOnDate(szczupak, LocalDate.of(2026, 5, 1)));
        assertFalse(fishMapper.isProtectedOnDate(szczupak, LocalDate.of(2026, 12, 31)));

        // Range crossing year: 1 grudnia - 31 marca (np. węgorz)
        FishSpecies wegorz = FishSpecies.builder()
                .legalStatus(LegalStatus.COMMERCIAL_REGULATED)
                .protectionPeriod("1 grudnia - 31 marca")
                .build();

        assertTrue(fishMapper.isProtectedOnDate(wegorz, LocalDate.of(2026, 12, 15)));
        assertTrue(fishMapper.isProtectedOnDate(wegorz, LocalDate.of(2026, 1, 10)));
        assertTrue(fishMapper.isProtectedOnDate(wegorz, LocalDate.of(2026, 3, 31)));
        assertFalse(fishMapper.isProtectedOnDate(wegorz, LocalDate.of(2026, 4, 1)));
        assertFalse(fishMapper.isProtectedOnDate(wegorz, LocalDate.of(2026, 8, 20)));

        // Strictly protected species: always protected
        FishSpecies jesiotr = FishSpecies.builder()
                .legalStatus(LegalStatus.STRICTLY_PROTECTED)
                .protectionPeriod("Całoroczny")
                .build();

        assertTrue(fishMapper.isProtectedOnDate(jesiotr, LocalDate.of(2026, 7, 15)));
    }

    @Test
    @DisplayName("Should map create/update DTO to entity and update fields properly")
    void shouldMapCreateUpdateDtoToEntity() {
        FishSpeciesCreateUpdateDto dto = FishSpeciesCreateUpdateDto.builder()
                .name("Okoń pospolity")
                .latinName("Perca fluviatilis")
                .family("Okoniowate (Percidae)")
                .fishType(FishType.PREDATOR)
                .legalStatus(LegalStatus.NATIVE)
                .minSizeLimit(18)
                .recordWeightKg(2.69)
                .activeMonths("1, 2, 3, 8, 9, 10, 11, 12")
                .effectiveMethods("Spinning, Podlódka, Spławik")
                .build();

        FishSpecies entity = fishMapper.toEntity(dto);

        assertNotNull(entity);
        assertEquals("Okoń pospolity", entity.getName());
        assertEquals("Perca fluviatilis", entity.getLatinName());
        assertEquals("okon-pospolity", entity.getSlug());
        assertEquals(FishType.PREDATOR, entity.getFishType());
        assertEquals(18, entity.getMinSizeLimit());
        assertEquals(2.69, entity.getRecordWeightKg());
    }
}
