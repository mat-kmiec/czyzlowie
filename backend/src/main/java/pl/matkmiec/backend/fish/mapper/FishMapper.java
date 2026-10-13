package pl.matkmiec.backend.fish.mapper;

import org.springframework.stereotype.Component;
import pl.matkmiec.backend.fish.dto.*;
import pl.matkmiec.backend.fish.model.FishSpecies;
import pl.matkmiec.backend.fish.model.LegalStatus;

import java.time.LocalDate;
import java.time.MonthDay;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class FishMapper {

    private static final Map<String, Integer> POLISH_MONTHS = new LinkedHashMap<>();

    static {
        POLISH_MONTHS.put("stycznia", 1);
        POLISH_MONTHS.put("styczen", 1);
        POLISH_MONTHS.put("styczeń", 1);
        POLISH_MONTHS.put("lutego", 2);
        POLISH_MONTHS.put("luty", 2);
        POLISH_MONTHS.put("marca", 3);
        POLISH_MONTHS.put("marzec", 3);
        POLISH_MONTHS.put("kwietnia", 4);
        POLISH_MONTHS.put("kwiecien", 4);
        POLISH_MONTHS.put("kwiecień", 4);
        POLISH_MONTHS.put("maja", 5);
        POLISH_MONTHS.put("maj", 5);
        POLISH_MONTHS.put("czerwca", 6);
        POLISH_MONTHS.put("czerwiec", 6);
        POLISH_MONTHS.put("lipca", 7);
        POLISH_MONTHS.put("lipiec", 7);
        POLISH_MONTHS.put("sierpnia", 8);
        POLISH_MONTHS.put("sierpien", 8);
        POLISH_MONTHS.put("sierpień", 8);
        POLISH_MONTHS.put("wrzesnia", 9);
        POLISH_MONTHS.put("września", 9);
        POLISH_MONTHS.put("wrzesien", 9);
        POLISH_MONTHS.put("wrzesień", 9);
        POLISH_MONTHS.put("pazdziernika", 10);
        POLISH_MONTHS.put("października", 10);
        POLISH_MONTHS.put("pazdziernik", 10);
        POLISH_MONTHS.put("październik", 10);
        POLISH_MONTHS.put("listopada", 11);
        POLISH_MONTHS.put("listopad", 11);
        POLISH_MONTHS.put("grudnia", 12);
        POLISH_MONTHS.put("grudzien", 12);
        POLISH_MONTHS.put("grudzień", 12);
    }

    public FishSpeciesListItemDto toListItemDto(FishSpecies entity, LocalDate referenceDate) {
        if (entity == null) {
            return null;
        }

        LocalDate targetDate = referenceDate != null ? referenceDate : LocalDate.now();
        boolean isProtected = isProtectedOnDate(entity, targetDate);

        return FishSpeciesListItemDto.builder()
                .id(entity.getId())
                .slug(entity.getSlug())
                .name(entity.getName())
                .latinName(entity.getLatinName())
                .family(entity.getFamily())
                .fishType(entity.getFishType())
                .fishTypeDisplayName(entity.getFishType() != null ? entity.getFishType().getDisplayName() : null)
                .imageUrl(entity.getImageUrl())
                .thumbnailUrl(entity.getThumbnailUrl())
                .shortDescription(entity.getShortDescription())
                .minSizeLimit(entity.getMinSizeLimit())
                .maxSizeLimit(entity.getMaxSizeLimit())
                .protectionPeriod(entity.getProtectionPeriod())
                .dailyLimit(entity.getDailyLimit())
                .legalStatus(entity.getLegalStatus())
                .legalStatusDisplayName(entity.getLegalStatus() != null ? entity.getLegalStatus().getDisplayName() : null)
                .isProtectedNow(isProtected)
                .waterZone(entity.getWaterZone())
                .waterZoneDisplayName(entity.getWaterZone() != null ? entity.getWaterZone().getDisplayName() : null)
                .difficulty(entity.getDifficulty())
                .difficultyDisplayName(entity.getDifficulty() != null ? entity.getDifficulty().getDisplayName() : null)
                .recordWeightKg(entity.getRecordWeightKg())
                .recordLengthCm(entity.getRecordLengthCm())
                .activeMonths(parseActiveMonths(entity.getActiveMonths()))
                .leaderRequired(entity.getLeaderRequired())
                .recommendedCastingWeight(entity.getRecommendedCastingWeight())
                .recommendedHookSize(entity.getRecommendedHookSize())
                .hasSafetyAlerts(entity.getSafetyAlerts() != null && !entity.getSafetyAlerts().isBlank())
                .safetyAlertsRaw(entity.getSafetyAlerts())
                .build();
    }

    public FishSpeciesDetailsDto toDetailsDto(FishSpecies entity, LocalDate referenceDate) {
        if (entity == null) {
            return null;
        }

        LocalDate targetDate = referenceDate != null ? referenceDate : LocalDate.now();
        boolean isProtected = isProtectedOnDate(entity, targetDate);

        FishBasicInfoDto basicInfo = FishBasicInfoDto.builder()
                .name(entity.getName())
                .latinName(entity.getLatinName())
                .family(entity.getFamily())
                .imageUrl(entity.getImageUrl())
                .thumbnailUrl(entity.getThumbnailUrl())
                .shortDescription(entity.getShortDescription())
                .description(entity.getDescription())
                .identificationFeatures(entity.getIdentificationFeatures())
                .build();

        FishLegalInfoDto legalInfo = FishLegalInfoDto.builder()
                .minSizeLimit(entity.getMinSizeLimit())
                .maxSizeLimit(entity.getMaxSizeLimit())
                .protectionPeriod(entity.getProtectionPeriod())
                .dailyLimit(entity.getDailyLimit())
                .legalStatus(entity.getLegalStatus())
                .legalStatusDisplayName(entity.getLegalStatus() != null ? entity.getLegalStatus().getDisplayName() : null)
                .legalStatusDescription(entity.getLegalStatus() != null ? entity.getLegalStatus().getDescription() : null)
                .legalNotes(entity.getLegalNotes())
                .isProtectedNow(isProtected)
                .build();

        FishRecordDto recordDto = FishRecordDto.builder()
                .weightKg(entity.getRecordWeightKg())
                .lengthCm(entity.getRecordLengthCm())
                .year(entity.getRecordYear())
                .angler(entity.getRecordAngler())
                .build();

        FishBiologyDto biologyAndRecords = FishBiologyDto.builder()
                .record(recordDto)
                .averageWeight(entity.getAverageWeight())
                .averageLength(entity.getAverageLength())
                .waterPreferences(splitToList(entity.getWaterPreferences()))
                .waterZone(entity.getWaterZone())
                .waterZoneDisplayName(entity.getWaterZone() != null ? entity.getWaterZone().getDisplayName() : null)
                .waterZoneDescription(entity.getWaterZone() != null ? entity.getWaterZone().getDescription() : null)
                .naturalFood(entity.getNaturalFood())
                .spawningMonths(entity.getSpawningMonths())
                .similarSpecies(entity.getSimilarSpecies())
                .build();

        FishTacticsDto tacticsAndTips = FishTacticsDto.builder()
                .dailyActivity(entity.getDailyActivity())
                .seasonalActivity(entity.getSeasonalActivity())
                .activeMonths(parseActiveMonths(entity.getActiveMonths()))
                .weatherPreferences(entity.getWeatherPreferences())
                .effectiveMethods(splitToList(entity.getEffectiveMethods()))
                .bestArtificialBaits(entity.getBestArtificialBaits())
                .bestNaturalBaits(entity.getBestNaturalBaits())
                .biteAndFightCharacteristics(entity.getBiteAndFightCharacteristics())
                .typicalSpots(entity.getTypicalSpots())
                .difficulty(entity.getDifficulty())
                .difficultyDisplayName(entity.getDifficulty() != null ? entity.getDifficulty().getDisplayName() : null)
                .difficultyDescription(entity.getDifficulty() != null ? entity.getDifficulty().getDescription() : null)
                .build();

        FishTackleGuideDto tackleGuide = toTackleGuideDto(entity);
        FishCatchAndReleaseDto catchAndReleaseGuide = toCatchAndReleaseDto(entity);

        return FishSpeciesDetailsDto.builder()
                .id(entity.getId())
                .slug(entity.getSlug())
                .name(entity.getName())
                .latinName(entity.getLatinName())
                .family(entity.getFamily())
                .fishType(entity.getFishType())
                .fishTypeDisplayName(entity.getFishType() != null ? entity.getFishType().getDisplayName() : null)
                .fishTypeDescription(entity.getFishType() != null ? entity.getFishType().getDescription() : null)
                .imageUrl(entity.getImageUrl())
                .thumbnailUrl(entity.getThumbnailUrl())
                .shortDescription(entity.getShortDescription())
                .legalStatus(entity.getLegalStatus())
                .legalStatusDisplayName(entity.getLegalStatus() != null ? entity.getLegalStatus().getDisplayName() : null)
                .difficulty(entity.getDifficulty())
                .difficultyDisplayName(entity.getDifficulty() != null ? entity.getDifficulty().getDisplayName() : null)
                .isProtectedNow(isProtected)
                .basicInfo(basicInfo)
                .legalInfo(legalInfo)
                .biologyAndRecords(biologyAndRecords)
                .tacticsAndTips(tacticsAndTips)
                .tackleGuide(tackleGuide)
                .catchAndReleaseGuide(catchAndReleaseGuide)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public FishRecordLeaderboardDto toRecordLeaderboardDto(FishSpecies entity) {
        if (entity == null) {
            return null;
        }

        return FishRecordLeaderboardDto.builder()
                .id(entity.getId())
                .slug(entity.getSlug())
                .name(entity.getName())
                .latinName(entity.getLatinName())
                .family(entity.getFamily())
                .fishType(entity.getFishType() != null ? entity.getFishType().getDisplayName() : null)
                .imageUrl(entity.getImageUrl())
                .recordWeightKg(entity.getRecordWeightKg())
                .recordLengthCm(entity.getRecordLengthCm())
                .recordYear(entity.getRecordYear())
                .recordAngler(entity.getRecordAngler())
                .build();
    }

    public FishSearchSuggestionDto toSearchSuggestionDto(FishSpecies entity) {
        if (entity == null) {
            return null;
        }

        return FishSearchSuggestionDto.builder()
                .id(entity.getId())
                .slug(entity.getSlug())
                .name(entity.getName())
                .latinName(entity.getLatinName())
                .family(entity.getFamily())
                .fishType(entity.getFishType())
                .fishTypeDisplayName(entity.getFishType() != null ? entity.getFishType().getDisplayName() : null)
                .thumbnailUrl(entity.getThumbnailUrl() != null ? entity.getThumbnailUrl() : entity.getImageUrl())
                .build();
    }

    public FishTackleGuideDto toTackleGuideDto(FishSpecies entity) {
        if (entity == null) {
            return null;
        }

        return FishTackleGuideDto.builder()
                .lineMonofilament(entity.getLineMonofilament())
                .lineBraid(entity.getLineBraid())
                .recommendedHookSize(entity.getRecommendedHookSize())
                .recommendedCastingWeight(entity.getRecommendedCastingWeight())
                .recommendedRodAction(entity.getRecommendedRodAction())
                .leaderRequired(entity.getLeaderRequired())
                .recommendedLeader(entity.getRecommendedLeader())
                .additionalGearTips(entity.getAdditionalGearTips())
                .build();
    }

    public FishCatchAndReleaseDto toCatchAndReleaseDto(FishSpecies entity) {
        if (entity == null) {
            return null;
        }

        boolean hasSafety = entity.getSafetyAlerts() != null && !entity.getSafetyAlerts().isBlank();

        return FishCatchAndReleaseDto.builder()
                .landingMethod(entity.getLandingMethod())
                .requiredGear(splitToList(entity.getRequiredGearCAndR()))
                .requiredGearRaw(entity.getRequiredGearCAndR())
                .safetyAlerts(splitToList(entity.getSafetyAlerts()))
                .safetyAlertsRaw(entity.getSafetyAlerts())
                .hasSafetyAlerts(hasSafety)
                .specialCareTips(entity.getSpecialCareTips())
                .barblessHooksRecommended(entity.getBarblessHooksRecommended())
                .build();
    }

    public FishTackleSummaryDto toTackleSummaryDto(FishSpecies entity) {
        if (entity == null) {
            return null;
        }

        return FishTackleSummaryDto.builder()
                .id(entity.getId())
                .slug(entity.getSlug())
                .name(entity.getName())
                .latinName(entity.getLatinName())
                .family(entity.getFamily())
                .fishType(entity.getFishType())
                .fishTypeDisplayName(entity.getFishType() != null ? entity.getFishType().getDisplayName() : null)
                .thumbnailUrl(entity.getThumbnailUrl() != null ? entity.getThumbnailUrl() : entity.getImageUrl())
                .tackleGuide(toTackleGuideDto(entity))
                .build();
    }

    public FishSafetyAlertDto toSafetyAlertDto(FishSpecies entity) {
        if (entity == null) {
            return null;
        }

        boolean hasSafety = entity.getSafetyAlerts() != null && !entity.getSafetyAlerts().isBlank();

        return FishSafetyAlertDto.builder()
                .id(entity.getId())
                .slug(entity.getSlug())
                .name(entity.getName())
                .latinName(entity.getLatinName())
                .family(entity.getFamily())
                .fishType(entity.getFishType())
                .fishTypeDisplayName(entity.getFishType() != null ? entity.getFishType().getDisplayName() : null)
                .thumbnailUrl(entity.getThumbnailUrl() != null ? entity.getThumbnailUrl() : entity.getImageUrl())
                .hasSafetyAlerts(hasSafety)
                .safetyAlerts(splitToList(entity.getSafetyAlerts()))
                .safetyAlertsRaw(entity.getSafetyAlerts())
                .landingMethod(entity.getLandingMethod())
                .requiredGear(splitToList(entity.getRequiredGearCAndR()))
                .requiredGearRaw(entity.getRequiredGearCAndR())
                .specialCareTips(entity.getSpecialCareTips())
                .build();
    }

    public FishSpecies toEntity(FishSpeciesCreateUpdateDto dto) {
        if (dto == null) {
            return null;
        }

        FishSpecies entity = new FishSpecies();
        updateEntityFromDto(entity, dto);
        return entity;
    }

    public void updateEntityFromDto(FishSpecies entity, FishSpeciesCreateUpdateDto dto) {
        if (dto == null || entity == null) {
            return;
        }

        entity.setSlug(dto.getSlug() != null && !dto.getSlug().isBlank() ? dto.getSlug() : FishSpecies.generateSlug(dto.getName()));
        entity.setName(dto.getName());
        entity.setLatinName(dto.getLatinName());
        entity.setFamily(dto.getFamily());
        entity.setFishType(dto.getFishType());
        entity.setImageUrl(dto.getImageUrl());
        entity.setThumbnailUrl(dto.getThumbnailUrl());
        entity.setShortDescription(dto.getShortDescription());
        entity.setDescription(dto.getDescription());
        entity.setIdentificationFeatures(dto.getIdentificationFeatures());

        entity.setMinSizeLimit(dto.getMinSizeLimit());
        entity.setMaxSizeLimit(dto.getMaxSizeLimit());
        entity.setProtectionPeriod(dto.getProtectionPeriod());
        entity.setDailyLimit(dto.getDailyLimit());
        entity.setLegalStatus(dto.getLegalStatus());
        entity.setLegalNotes(dto.getLegalNotes());

        entity.setRecordWeightKg(dto.getRecordWeightKg());
        entity.setRecordLengthCm(dto.getRecordLengthCm());
        entity.setRecordYear(dto.getRecordYear());
        entity.setRecordAngler(dto.getRecordAngler());
        entity.setAverageWeight(dto.getAverageWeight());
        entity.setAverageLength(dto.getAverageLength());
        entity.setWaterPreferences(dto.getWaterPreferences());
        entity.setWaterZone(dto.getWaterZone());
        entity.setNaturalFood(dto.getNaturalFood());
        entity.setSpawningMonths(dto.getSpawningMonths());
        entity.setActiveMonths(dto.getActiveMonths());
        entity.setSimilarSpecies(dto.getSimilarSpecies());

        entity.setDailyActivity(dto.getDailyActivity());
        entity.setSeasonalActivity(dto.getSeasonalActivity());
        entity.setWeatherPreferences(dto.getWeatherPreferences());
        entity.setEffectiveMethods(dto.getEffectiveMethods());
        entity.setBestArtificialBaits(dto.getBestArtificialBaits());
        entity.setBestNaturalBaits(dto.getBestNaturalBaits());
        entity.setBiteAndFightCharacteristics(dto.getBiteAndFightCharacteristics());
        entity.setTypicalSpots(dto.getTypicalSpots());
        entity.setDifficulty(dto.getDifficulty());

        // V. Moduł Sprzętowy
        entity.setLineMonofilament(dto.getLineMonofilament());
        entity.setLineBraid(dto.getLineBraid());
        entity.setRecommendedHookSize(dto.getRecommendedHookSize());
        entity.setRecommendedCastingWeight(dto.getRecommendedCastingWeight());
        entity.setRecommendedRodAction(dto.getRecommendedRodAction());
        entity.setLeaderRequired(dto.getLeaderRequired());
        entity.setRecommendedLeader(dto.getRecommendedLeader());
        entity.setAdditionalGearTips(dto.getAdditionalGearTips());

        // VI. Moduł Catch & Release i Bezpieczeństwo
        entity.setLandingMethod(dto.getLandingMethod());
        entity.setRequiredGearCAndR(dto.getRequiredGearCAndR());
        entity.setSafetyAlerts(dto.getSafetyAlerts());
        entity.setSpecialCareTips(dto.getSpecialCareTips());
        entity.setBarblessHooksRecommended(dto.getBarblessHooksRecommended());

        if (dto.getDisplayOrder() != null) {
            entity.setDisplayOrder(dto.getDisplayOrder());
        }
        if (dto.getIsActive() != null) {
            entity.setIsActive(dto.getIsActive());
        }
    }

    public List<String> splitToList(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(text.split("[,;\\n•]"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
    }

    public List<Integer> parseActiveMonths(String activeMonthsStr) {
        if (activeMonthsStr == null || activeMonthsStr.isBlank()) {
            return Collections.emptyList();
        }

        List<Integer> months = new ArrayList<>();
        String[] parts = activeMonthsStr.split("[,;\\s]+");
        for (String part : parts) {
            try {
                int month = Integer.parseInt(part.trim());
                if (month >= 1 && month <= 12) {
                    months.add(month);
                }
            } catch (NumberFormatException ignored) {
            }
        }
        Collections.sort(months);
        return months;
    }

    public boolean isProtectedOnDate(FishSpecies entity, LocalDate date) {
        if (entity == null || date == null) {
            return false;
        }

        if (entity.getLegalStatus() == LegalStatus.STRICTLY_PROTECTED) {
            return true;
        }

        String period = entity.getProtectionPeriod();
        if (period == null || period.isBlank() || period.equalsIgnoreCase("brak") || period.equalsIgnoreCase("brak okresu ochronnego")) {
            return false;
        }

        return isDateInProtectionRange(period.toLowerCase(), date);
    }

    private boolean isDateInProtectionRange(String periodText, LocalDate date) {
        // Pattern like "1 stycznia - 30 kwietnia" or "1.01 - 30.04" or "1 listopada do końca lutego"
        // Also handles multiple periods separated by "oraz", e.g. "1 sty - 30 kwi oraz 1 wrz - 30 lis"
        String normalized = periodText.toLowerCase()
                .replace("–", "-")
                .replace("—", "-")
                .replace(" do ", " - ");

        String[] ranges = normalized.split("(oraz|,|;) ");
        for (String range : ranges) {
            if (checkSingleRange(range.trim(), date)) {
                return true;
            }
        }
        return false;
    }

    private boolean checkSingleRange(String range, LocalDate date) {
        String[] parts = range.split("-");
        if (parts.length != 2) {
            return false;
        }

        MonthDay start = parsePolishMonthDay(parts[0].trim());
        MonthDay end = parsePolishMonthDay(parts[1].trim());

        if (start == null || end == null) {
            return false;
        }

        MonthDay current = MonthDay.of(date.getMonth(), date.getDayOfMonth());

        if (start.compareTo(end) <= 0) {
            // e.g. 1 stycznia - 30 kwietnia
            return (current.compareTo(start) >= 0 && current.compareTo(end) <= 0);
        } else {
            // Crosses new year, e.g. 1 listopada - 28 lutego
            return (current.compareTo(start) >= 0 || current.compareTo(end) <= 0);
        }
    }

    private MonthDay parsePolishMonthDay(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }

        // Check format DD.MM (e.g. 01.01 or 30.04)
        Pattern dotPattern = Pattern.compile("(\\d{1,2})\\.(\\d{1,2})");
        Matcher dotMatcher = dotPattern.matcher(token);
        if (dotMatcher.find()) {
            int day = Integer.parseInt(dotMatcher.group(1));
            int month = Integer.parseInt(dotMatcher.group(2));
            return createSafeMonthDay(month, day);
        }

        // Check format: "1 stycznia" or "końca lutego"
        Pattern textPattern = Pattern.compile("(\\d{1,2}|konca|końca)?\\s*([a-ząćęłńóśźż]+)");
        Matcher textMatcher = textPattern.matcher(token);
        if (textMatcher.find()) {
            String dayStr = textMatcher.group(1);
            String monthName = textMatcher.group(2);

            Integer month = findMonthNumber(monthName);
            if (month == null) {
                return null;
            }

            int day = 1;
            if (dayStr != null) {
                if (dayStr.contains("konc") || dayStr.contains("końc")) {
                    day = java.time.Month.of(month).maxLength();
                } else {
                    try {
                        day = Integer.parseInt(dayStr);
                    } catch (NumberFormatException e) {
                        day = 1;
                    }
                }
            } else if (token.contains("koniec") || token.contains("końca")) {
                day = java.time.Month.of(month).maxLength();
            }

            return createSafeMonthDay(month, day);
        }

        return null;
    }

    private MonthDay createSafeMonthDay(int month, int day) {
        if (month < 1 || month > 12) return null;
        int maxDay = java.time.Month.of(month).maxLength();
        int safeDay = Math.min(Math.max(1, day), maxDay);
        return MonthDay.of(month, safeDay);
    }

    private Integer findMonthNumber(String monthWord) {
        if (monthWord == null) return null;
        String clean = monthWord.toLowerCase().trim();
        for (Map.Entry<String, Integer> entry : POLISH_MONTHS.entrySet()) {
            if (clean.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }
}
