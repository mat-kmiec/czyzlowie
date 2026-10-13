package pl.matkmiec.backend.fish.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pl.matkmiec.backend.fish.dto.*;
import pl.matkmiec.backend.fish.model.DifficultyLevel;
import pl.matkmiec.backend.fish.model.FishType;
import pl.matkmiec.backend.fish.model.LegalStatus;
import pl.matkmiec.backend.fish.model.WaterZone;
import pl.matkmiec.backend.fish.service.FishQueryService;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FishPublicControllerTest {

    private MockMvc mockMvc;

    @Mock
    private FishQueryService fishQueryService;

    @InjectMocks
    private FishPublicController fishPublicController;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper);

        mockMvc = MockMvcBuilders.standaloneSetup(fishPublicController)
                .setMessageConverters(converter)
                .build();
    }

    @Test
    @DisplayName("GET /v1/fish should return list of fish species")
    void shouldReturnFishSpeciesList() throws Exception {
        FishSpeciesListItemDto item = FishSpeciesListItemDto.builder()
                .id(1L)
                .slug("szczupak-pospolity")
                .name("Szczupak pospolity")
                .latinName("Esox lucius")
                .fishType(FishType.PREDATOR)
                .fishTypeDisplayName("Drapieżnik")
                .minSizeLimit(50)
                .maxSizeLimit(90)
                .protectionPeriod("1 stycznia - 30 kwietnia")
                .isProtectedNow(true)
                .build();

        when(fishQueryService.getSpeciesList(any(), any(), any(), any(), any()))
                .thenReturn(List.of(item));

        mockMvc.perform(get("/v1/fish")
                        .param("type", "PREDATOR")
                        .param("search", "szczupak"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("Szczupak pospolity"))
                .andExpect(jsonPath("$[0].slug").value("szczupak-pospolity"))
                .andExpect(jsonPath("$[0].fishType").value("PREDATOR"))
                .andExpect(jsonPath("$[0].isProtectedNow").value(true));
    }

    @Test
    @DisplayName("GET /v1/fish/{idOrSlug} should return species details with 4 sections")
    void shouldReturnSpeciesDetails() throws Exception {
        FishBasicInfoDto basicInfo = FishBasicInfoDto.builder()
                .name("Sandacz")
                .latinName("Sander lucioperca")
                .family("Okoniowate (Percidae)")
                .description("Opis sandacza")
                .identificationFeatures("Kły i szkliste oczy")
                .build();

        FishLegalInfoDto legalInfo = FishLegalInfoDto.builder()
                .minSizeLimit(50)
                .protectionPeriod("1 stycznia - 31 maja")
                .legalStatus(LegalStatus.NATIVE)
                .legalStatusDisplayName("Gatunek rodzimy (regulowany RAPR/PZW)")
                .build();

        FishBiologyDto biology = FishBiologyDto.builder()
                .record(FishRecordDto.builder().weightKg(15.60).year(1980).build())
                .waterZone(WaterZone.BOTTOM)
                .waterZoneDisplayName("Przydenna (Dno / Bentos)")
                .build();

        FishTacticsDto tactics = FishTacticsDto.builder()
                .difficulty(DifficultyLevel.HARD)
                .difficultyDisplayName("Trudny")
                .effectiveMethods(List.of("Spinning z opadu", "Wobler nocny"))
                .build();

        FishSpeciesDetailsDto details = FishSpeciesDetailsDto.builder()
                .id(2L)
                .slug("sandacz")
                .name("Sandacz")
                .latinName("Sander lucioperca")
                .fishType(FishType.PREDATOR)
                .basicInfo(basicInfo)
                .legalInfo(legalInfo)
                .biologyAndRecords(biology)
                .tacticsAndTips(tactics)
                .build();

        when(fishQueryService.getSpeciesByIdOrSlug(eq("sandacz"), any()))
                .thenReturn(details);

        mockMvc.perform(get("/v1/fish/sandacz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Sandacz"))
                .andExpect(jsonPath("$.slug").value("sandacz"))
                .andExpect(jsonPath("$.basicInfo.family").value("Okoniowate (Percidae)"))
                .andExpect(jsonPath("$.legalInfo.minSizeLimit").value(50))
                .andExpect(jsonPath("$.biologyAndRecords.record.weightKg").value(15.60))
                .andExpect(jsonPath("$.tacticsAndTips.effectiveMethods[0]").value("Spinning z opadu"));
    }

    @Test
    @DisplayName("GET /v1/fish/categories should return category summary")
    void shouldReturnCategories() throws Exception {
        FishCategorySummaryDto summary = FishCategorySummaryDto.builder()
                .fishType(FishType.PREDATOR)
                .name("Drapieżnik")
                .speciesCount(5L)
                .build();

        when(fishQueryService.getCategoriesSummary(any())).thenReturn(List.of(summary));

        mockMvc.perform(get("/v1/fish/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fishType").value("PREDATOR"))
                .andExpect(jsonPath("$[0].speciesCount").value(5));
    }

    @Test
    @DisplayName("GET /v1/fish/calendar should return fishing calendar")
    void shouldReturnCalendar() throws Exception {
        FishCalendarMonthDto calendar = FishCalendarMonthDto.builder()
                .monthNumber(5)
                .monthName("Maj")
                .season("Wiosna")
                .activeSpecies(List.of())
                .protectedSpecies(List.of())
                .build();

        when(fishQueryService.getCalendarMonth(eq(5), any())).thenReturn(calendar);

        mockMvc.perform(get("/v1/fish/calendar").param("month", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthNumber").value(5))
                .andExpect(jsonPath("$.monthName").value("Maj"));
    }

    @Test
    @DisplayName("GET /v1/fish/records should return leaderboard")
    void shouldReturnRecords() throws Exception {
        FishRecordLeaderboardDto rec = FishRecordLeaderboardDto.builder()
                .name("Karp")
                .recordWeightKg(34.50)
                .recordYear(2020)
                .build();

        when(fishQueryService.getRecordLeaderboard()).thenReturn(List.of(rec));

        mockMvc.perform(get("/v1/fish/records"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Karp"))
                .andExpect(jsonPath("$[0].recordWeightKg").value(34.50));
    }

    @Test
    @DisplayName("GET /v1/fish/search should return autocomplete suggestions")
    void shouldReturnAutocomplete() throws Exception {
        FishSearchSuggestionDto sugg = FishSearchSuggestionDto.builder()
                .name("Okoń pospolity")
                .slug("okon-pospolity")
                .build();

        when(fishQueryService.searchAutocomplete(eq("oko"), eq(8))).thenReturn(List.of(sugg));

        mockMvc.perform(get("/v1/fish/search").param("query", "oko"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Okoń pospolity"));
    }

    @Test
    @DisplayName("GET /v1/fish/{idOrSlug}/tackle should return tackle parameters guide")
    void shouldReturnSpeciesTackleGuide() throws Exception {
        FishTackleGuideDto tackle = FishTackleGuideDto.builder()
                .lineMonofilament("0.28 - 0.35 mm")
                .lineBraid("0.14 - 0.20 mm")
                .recommendedHookSize("#2/0 dla szczupaka")
                .recommendedCastingWeight("15 - 50g")
                .leaderRequired(true)
                .recommendedLeader("Przypon wolframowy min. 30 cm")
                .build();

        when(fishQueryService.getTackleGuide("szczupak-pospolity")).thenReturn(tackle);

        mockMvc.perform(get("/v1/fish/szczupak-pospolity/tackle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineMonofilament").value("0.28 - 0.35 mm"))
                .andExpect(jsonPath("$.leaderRequired").value(true))
                .andExpect(jsonPath("$.recommendedCastingWeight").value("15 - 50g"));
    }

    @Test
    @DisplayName("GET /v1/fish/{idOrSlug}/catch-and-release should return C&R and safety guide")
    void shouldReturnSpeciesCatchAndReleaseGuide() throws Exception {
        FishCatchAndReleaseDto cr = FishCatchAndReleaseDto.builder()
                .landingMethod("Podbierak z siatką gumowaną")
                .hasSafetyAlerts(true)
                .safetyAlerts(List.of("Ostre zęby"))
                .requiredGear(List.of("Mata karpiowa", "Długie szczypce"))
                .barblessHooksRecommended(true)
                .build();

        when(fishQueryService.getCatchAndReleaseGuide("szczupak-pospolity")).thenReturn(cr);

        mockMvc.perform(get("/v1/fish/szczupak-pospolity/catch-and-release"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.landingMethod").value("Podbierak z siatką gumowaną"))
                .andExpect(jsonPath("$.hasSafetyAlerts").value(true))
                .andExpect(jsonPath("$.barblessHooksRecommended").value(true))
                .andExpect(jsonPath("$.safetyAlerts[0]").value("Ostre zęby"));
    }

    @Test
    @DisplayName("GET /v1/fish/tackle-guide should return all tackle guides")
    void shouldReturnAllTackleGuides() throws Exception {
        FishTackleSummaryDto summary = FishTackleSummaryDto.builder()
                .id(1L)
                .slug("okon-pospolity")
                .name("Okoń pospolity")
                .fishType(FishType.PREDATOR)
                .tackleGuide(FishTackleGuideDto.builder().recommendedCastingWeight("Ultra Light 1-5g").build())
                .build();

        when(fishQueryService.getAllTackleGuides(any())).thenReturn(List.of(summary));

        mockMvc.perform(get("/v1/fish/tackle-guide"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("okon-pospolity"))
                .andExpect(jsonPath("$[0].tackleGuide.recommendedCastingWeight").value("Ultra Light 1-5g"));
    }

    @Test
    @DisplayName("GET /v1/fish/safety-alerts should return safety alerts list")
    void shouldReturnSafetyAlerts() throws Exception {
        FishSafetyAlertDto alert = FishSafetyAlertDto.builder()
                .slug("sumik-karlowaty")
                .name("Sumik karłowaty")
                .hasSafetyAlerts(true)
                .safetyAlerts(List.of("Kolce jadowe"))
                .build();

        when(fishQueryService.getSafetyAlerts()).thenReturn(List.of(alert));

        mockMvc.perform(get("/v1/fish/safety-alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("sumik-karlowaty"))
                .andExpect(jsonPath("$[0].safetyAlerts[0]").value("Kolce jadowe"));
    }
}
