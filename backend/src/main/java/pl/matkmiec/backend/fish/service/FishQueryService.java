package pl.matkmiec.backend.fish.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.matkmiec.backend.fish.dto.*;
import pl.matkmiec.backend.fish.exception.FishSpeciesNotFoundException;
import pl.matkmiec.backend.fish.mapper.FishMapper;
import pl.matkmiec.backend.fish.model.*;
import pl.matkmiec.backend.fish.repository.FishSpeciesRepository;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class FishQueryService {

    private final FishSpeciesRepository fishSpeciesRepository;
    private final FishMapper fishMapper;

    public List<FishSpeciesListItemDto> getSpeciesList(
            FishType fishType,
            LegalStatus legalStatus,
            String search,
            Integer activeMonth,
            LocalDate date
    ) {
        LocalDate referenceDate = date != null ? date : LocalDate.now();

        List<FishSpecies> speciesList;
        if (search != null && !search.isBlank()) {
            speciesList = fishSpeciesRepository.searchActiveSpecies(search.trim());
        } else if (fishType != null) {
            speciesList = fishSpeciesRepository.findByFishTypeAndIsActiveTrueOrderByDisplayOrderAscNameAsc(fishType);
        } else if (legalStatus != null) {
            speciesList = fishSpeciesRepository.findByLegalStatusAndIsActiveTrueOrderByDisplayOrderAscNameAsc(legalStatus);
        } else {
            speciesList = fishSpeciesRepository.findByIsActiveTrueOrderByDisplayOrderAscNameAsc();
        }

        // Dodatkowe filtry w pamięci jeśli podano jednocześnie kombinację (np. fishType + search lub activeMonth)
        if (fishType != null && search != null && !search.isBlank()) {
            speciesList = speciesList.stream()
                    .filter(f -> f.getFishType() == fishType)
                    .toList();
        }

        if (legalStatus != null && (search != null && !search.isBlank() || fishType != null)) {
            speciesList = speciesList.stream()
                    .filter(f -> f.getLegalStatus() == legalStatus)
                    .toList();
        }

        if (activeMonth != null) {
            speciesList = speciesList.stream()
                    .filter(f -> {
                        List<Integer> months = fishMapper.parseActiveMonths(f.getActiveMonths());
                        return months.contains(activeMonth);
                    })
                    .toList();
        }

        return speciesList.stream()
                .map(f -> fishMapper.toListItemDto(f, referenceDate))
                .toList();
    }

    public FishSpeciesDetailsDto getSpeciesByIdOrSlug(String idOrSlug, LocalDate date) {
        LocalDate referenceDate = date != null ? date : LocalDate.now();
        FishSpecies species;

        if (idOrSlug.matches("\\d+")) {
            Long id = Long.parseLong(idOrSlug);
            species = fishSpeciesRepository.findById(id)
                    .orElseThrow(() -> new FishSpeciesNotFoundException(id));
        } else {
            String slug = idOrSlug.trim().toLowerCase();
            species = fishSpeciesRepository.findBySlug(slug)
                    .or(() -> fishSpeciesRepository.findByNameIgnoreCase(idOrSlug.trim()))
                    .orElseThrow(() -> new FishSpeciesNotFoundException("Nie znaleziono gatunku ryby: " + idOrSlug));
        }

        return fishMapper.toDetailsDto(species, referenceDate);
    }

    public List<FishCategorySummaryDto> getCategoriesSummary(LocalDate date) {
        LocalDate referenceDate = date != null ? date : LocalDate.now();
        List<FishSpecies> allActive = fishSpeciesRepository.findByIsActiveTrueOrderByDisplayOrderAscNameAsc();

        List<FishCategorySummaryDto> categories = new ArrayList<>();
        for (FishType type : FishType.values()) {
            List<FishSpecies> forType = allActive.stream()
                    .filter(f -> f.getFishType() == type)
                    .toList();

            List<FishSpeciesListItemDto> topList = forType.stream()
                    .limit(6)
                    .map(f -> fishMapper.toListItemDto(f, referenceDate))
                    .toList();

            categories.add(FishCategorySummaryDto.builder()
                    .fishType(type)
                    .name(type.getDisplayName())
                    .description(type.getDescription())
                    .speciesCount((long) forType.size())
                    .topSpecies(topList)
                    .build());
        }

        return categories;
    }

    public FishCalendarMonthDto getCalendarMonth(Integer monthNumber, LocalDate date) {
        LocalDate referenceDate = date != null ? date : LocalDate.now();
        int targetMonth = (monthNumber != null && monthNumber >= 1 && monthNumber <= 12)
                ? monthNumber
                : referenceDate.getMonthValue();

        LocalDate monthDate = referenceDate.withMonth(targetMonth).withDayOfMonth(15);
        Month monthEnum = Month.of(targetMonth);
        String monthName = monthEnum.getDisplayName(TextStyle.FULL_STANDALONE, Locale.forLanguageTag("pl"));
        String capitalizedMonthName = monthName.substring(0, 1).toUpperCase() + monthName.substring(1);

        String season = getSeasonForMonth(targetMonth);
        String tips = getSeasonalFishingTip(targetMonth);

        List<FishSpecies> allActive = fishSpeciesRepository.findByIsActiveTrueOrderByDisplayOrderAscNameAsc();

        List<FishSpeciesListItemDto> activeSpecies = allActive.stream()
                .filter(f -> {
                    List<Integer> months = fishMapper.parseActiveMonths(f.getActiveMonths());
                    return months.contains(targetMonth);
                })
                .map(f -> fishMapper.toListItemDto(f, monthDate))
                .toList();

        List<FishSpeciesListItemDto> protectedSpecies = allActive.stream()
                .filter(f -> fishMapper.isProtectedOnDate(f, monthDate))
                .map(f -> fishMapper.toListItemDto(f, monthDate))
                .toList();

        return FishCalendarMonthDto.builder()
                .monthNumber(targetMonth)
                .monthName(capitalizedMonthName)
                .season(season)
                .generalTips(tips)
                .activeSpecies(activeSpecies)
                .protectedSpecies(protectedSpecies)
                .build();
    }

    public List<FishRecordLeaderboardDto> getRecordLeaderboard() {
        return fishSpeciesRepository.findTopRecords().stream()
                .map(fishMapper::toRecordLeaderboardDto)
                .toList();
    }

    public FishSpeciesDetailsDto getRandomSpecies(LocalDate date) {
        LocalDate referenceDate = date != null ? date : LocalDate.now();
        FishSpecies species = fishSpeciesRepository.findRandomActiveSpecies()
                .orElseThrow(() -> new FishSpeciesNotFoundException("Brak dostępnych gatunków ryb w bazie"));

        return fishMapper.toDetailsDto(species, referenceDate);
    }

    public List<FishSearchSuggestionDto> searchAutocomplete(String query, int limit) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }
        int maxResults = Math.min(Math.max(limit, 1), 20);
        return fishSpeciesRepository.searchAutocomplete(query.trim(), PageRequest.of(0, maxResults))
                .stream()
                .map(fishMapper::toSearchSuggestionDto)
                .toList();
    }

    public FishProtectionOverviewDto getProtectionOverview(LocalDate date) {
        LocalDate referenceDate = date != null ? date : LocalDate.now();
        List<FishSpecies> all = fishSpeciesRepository.findByIsActiveTrueOrderByDisplayOrderAscNameAsc();

        List<FishSpeciesListItemDto> currentlyProtected = all.stream()
                .filter(f -> fishMapper.isProtectedOnDate(f, referenceDate))
                .map(f -> fishMapper.toListItemDto(f, referenceDate))
                .toList();

        List<FishSpeciesListItemDto> strictlyProtected = all.stream()
                .filter(f -> f.getLegalStatus() == LegalStatus.STRICTLY_PROTECTED)
                .map(f -> fishMapper.toListItemDto(f, referenceDate))
                .toList();

        List<FishSpeciesListItemDto> invasive = all.stream()
                .filter(f -> f.getLegalStatus() == LegalStatus.INVASIVE_NO_RELEASE)
                .map(f -> fishMapper.toListItemDto(f, referenceDate))
                .toList();

        List<FishSpeciesListItemDto> withSizeLimits = all.stream()
                .filter(f -> f.getMinSizeLimit() != null || f.getMaxSizeLimit() != null)
                .map(f -> fishMapper.toListItemDto(f, referenceDate))
                .toList();

        return FishProtectionOverviewDto.builder()
                .currentDate(referenceDate.toString())
                .currentMonth(referenceDate.getMonthValue())
                .currentlyProtectedSpecies(currentlyProtected)
                .strictlyProtectedSpecies(strictlyProtected)
                .invasiveSpeciesNoRelease(invasive)
                .speciesWithSizeLimits(withSizeLimits)
                .build();
    }

    public FishTackleGuideDto getTackleGuide(String idOrSlug) {
        FishSpecies species = findSpeciesEntity(idOrSlug);
        return fishMapper.toTackleGuideDto(species);
    }

    public FishCatchAndReleaseDto getCatchAndReleaseGuide(String idOrSlug) {
        FishSpecies species = findSpeciesEntity(idOrSlug);
        return fishMapper.toCatchAndReleaseDto(species);
    }

    public List<FishTackleSummaryDto> getAllTackleGuides(FishType type) {
        List<FishSpecies> speciesList;
        if (type != null) {
            speciesList = fishSpeciesRepository.findByFishTypeAndIsActiveTrueOrderByDisplayOrderAscNameAsc(type);
        } else {
            speciesList = fishSpeciesRepository.findByIsActiveTrueOrderByDisplayOrderAscNameAsc();
        }

        return speciesList.stream()
                .map(fishMapper::toTackleSummaryDto)
                .toList();
    }

    public List<FishSafetyAlertDto> getSafetyAlerts() {
        List<FishSpecies> allActive = fishSpeciesRepository.findByIsActiveTrueOrderByDisplayOrderAscNameAsc();
        return allActive.stream()
                .filter(f -> (f.getSafetyAlerts() != null && !f.getSafetyAlerts().isBlank()) ||
                             (f.getRequiredGearCAndR() != null && !f.getRequiredGearCAndR().isBlank()))
                .map(fishMapper::toSafetyAlertDto)
                .toList();
    }

    private FishSpecies findSpeciesEntity(String idOrSlug) {
        if (idOrSlug.matches("\\d+")) {
            Long id = Long.parseLong(idOrSlug);
            return fishSpeciesRepository.findById(id)
                    .orElseThrow(() -> new FishSpeciesNotFoundException(id));
        } else {
            String slug = idOrSlug.trim().toLowerCase();
            return fishSpeciesRepository.findBySlug(slug)
                    .or(() -> fishSpeciesRepository.findByNameIgnoreCase(idOrSlug.trim()))
                    .orElseThrow(() -> new FishSpeciesNotFoundException("Nie znaleziono gatunku ryby: " + idOrSlug));
        }
    }

    public List<FishEnumOptionDto> getFishTypes() {
        return Arrays.stream(FishType.values())
                .map(t -> FishEnumOptionDto.builder()
                        .code(t.name())
                        .displayName(t.getDisplayName())
                        .description(t.getDescription())
                        .build())
                .toList();
    }

    public List<FishEnumOptionDto> getLegalStatuses() {
        return Arrays.stream(LegalStatus.values())
                .map(s -> FishEnumOptionDto.builder()
                        .code(s.name())
                        .displayName(s.getDisplayName())
                        .description(s.getDescription())
                        .build())
                .toList();
    }

    public List<FishEnumOptionDto> getWaterZones() {
        return Arrays.stream(WaterZone.values())
                .map(z -> FishEnumOptionDto.builder()
                        .code(z.name())
                        .displayName(z.getDisplayName())
                        .description(z.getDescription())
                        .build())
                .toList();
    }

    public List<FishEnumOptionDto> getDifficultyLevels() {
        return Arrays.stream(DifficultyLevel.values())
                .map(d -> FishEnumOptionDto.builder()
                        .code(d.name())
                        .displayName(d.getDisplayName())
                        .description(d.getDescription())
                        .build())
                .toList();
    }

    private String getSeasonForMonth(int month) {
        return switch (month) {
            case 12, 1, 2 -> "Zima (połowy podlodowe i rzeczne zimowiska)";
            case 3, 4, 5 -> "Wiosna (przebudzenie po zimie, tarło i początek sezonu drapieżnikowego)";
            case 6, 7, 8 -> "Lato (maksimum aktywności białorybu, nocne żerowanie drapieżników)";
            case 9, 10, 11 -> "Jesień (szczyt żerowania szczupaka, sandacza i okonia przed zimą)";
            default -> "Sezon całoroczny";
        };
    }

    private String getSeasonalFishingTip(int month) {
        return switch (month) {
            case 1 -> "Styczeń: Czas na wędkarstwo podlodowe (okoń, płoć) oraz zimowe spinningowanie pstrąga potokowego na rzekach górskich i troci na rzekach pomorskich.";
            case 2 -> "Luty: Kontynuacja sezonu trociowego i pstrągowego. W wodach stojących ostrożne brania okoni i leszczy pod lodem.";
            case 3 -> "Marzec: Woda zaczyna się powoli nagrzewać. W płytszych, nasłonecznionych partiach rzek i jezior uaktywniają się jazie, klenie, płocie i jelce.";
            case 4 -> "Kwiecień: Doskonały czas na rzeczne klenie i jazie (groch, kukurydza, małe smużaki) oraz pierwsze wiosenne liny w płytkich zatokach.";
            case 5 -> "Maj: Otwarcie sezonu szczupakowego i boleniowego! Ryby po tarle intensywnie żerują na płyciznach i przy roślinności.";
            case 6 -> "Czerwiec: Otwarcie sezonu na sandacza i suma! Doskonałe wieczorne i nocne żerowanie, aktywne liny, karpie, amury i węgorze.";
            case 7 -> "Lipiec: Upały wymuszają wędkowanie o świcie, o zmierzchu oraz w nocy. Świetny okres na suma, węgorza, sandacza oraz powierzchniowego bolenia.";
            case 8 -> "Sierpień: Stabilne letnie żerowanie. Nocne wyprawy na sandacze z opaski oraz gruntowe zasiadki na karpie, liny i brzany w rzekach.";
            case 9 -> "Wrzesień: Początek jesiennego eldorado drapieżników. Szczupaki i okonie gromadzą się wokół stad drobnicy, woda się ochładza i klaruje.";
            case 10 -> "Październik: Szczyt sezonu spinningowego na grube szczupaki, sandacze z opadu i garbusy. Świetne żerowanie na głębszych blatach i spadach.";
            case 11 -> "Listopad: Głębokie łowienie sandaczy i okoni w rynnach i dołkach. Szczupaki polują na duże przynęty gumowe.";
            case 12 -> "Grudzień: Przygotowanie do zimy lub łowienie z lodu (jeśli jest bezpieczny). Aktywne miętusy podczas zimowych nocy oraz okonie z głębin.";
            default -> "Sprawdzaj bieżące warunki pogodowe, ciśnienie i fazy księżyca.";
        };
    }
}
