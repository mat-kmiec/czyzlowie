package pl.matkmiec.backend.fish.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import pl.matkmiec.backend.fish.model.DifficultyLevel;
import pl.matkmiec.backend.fish.model.FishType;
import pl.matkmiec.backend.fish.model.LegalStatus;
import pl.matkmiec.backend.fish.model.WaterZone;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FishSpeciesCreateUpdateDto {

    private String slug;

    @NotBlank(message = "Nazwa polska jest wymagana")
    private String name;

    @NotBlank(message = "Nazwa łacińska jest wymagana")
    private String latinName;

    @NotBlank(message = "Rodzina ryby jest wymagana")
    private String family;

    @NotNull(message = "Typ ryby jest wymagany")
    private FishType fishType;

    private String imageUrl;
    private String thumbnailUrl;
    private String shortDescription;
    private String description;
    private String identificationFeatures;

    // II. Regulamin i status prawny
    private Integer minSizeLimit;
    private Integer maxSizeLimit;
    private String protectionPeriod;
    private String dailyLimit;

    @NotNull(message = "Status prawny jest wymagany")
    private LegalStatus legalStatus;

    private String legalNotes;

    // III. Biologia i rekordy
    private Double recordWeightKg;
    private Double recordLengthCm;
    private Integer recordYear;
    private String recordAngler;
    private String averageWeight;
    private String averageLength;
    private String waterPreferences;
    private WaterZone waterZone;
    private String naturalFood;
    private String spawningMonths;
    private String activeMonths;
    private String similarSpecies;

    // IV. Wskazówki wędkarskie (Taktyka)
    private String dailyActivity;
    private String seasonalActivity;
    private String weatherPreferences;
    private String effectiveMethods;
    private String bestArtificialBaits;
    private String bestNaturalBaits;
    private String biteAndFightCharacteristics;
    private String typicalSpots;
    private DifficultyLevel difficulty;

    // V. Moduł Sprzętowy (Parametry techniczne)
    private String lineMonofilament;
    private String lineBraid;
    private String recommendedHookSize;
    private String recommendedCastingWeight;
    private String recommendedRodAction;
    private Boolean leaderRequired;
    private String recommendedLeader;
    private String additionalGearTips;

    // VI. Moduł Catch & Release i Bezpieczeństwo
    private String landingMethod;
    private String requiredGearCAndR;
    private String safetyAlerts;
    private String specialCareTips;
    private Boolean barblessHooksRecommended;

    private Integer displayOrder;
    private Boolean isActive;
}
