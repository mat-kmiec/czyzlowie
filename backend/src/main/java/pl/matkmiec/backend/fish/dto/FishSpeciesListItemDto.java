package pl.matkmiec.backend.fish.dto;

import lombok.*;
import pl.matkmiec.backend.fish.model.DifficultyLevel;
import pl.matkmiec.backend.fish.model.FishType;
import pl.matkmiec.backend.fish.model.LegalStatus;
import pl.matkmiec.backend.fish.model.WaterZone;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FishSpeciesListItemDto {
    private Long id;
    private String slug;
    private String name;
    private String latinName;
    private String family;
    private FishType fishType;
    private String fishTypeDisplayName;
    private String imageUrl;
    private String thumbnailUrl;
    private String shortDescription;

    // Skrócone regulacje
    private Integer minSizeLimit;
    private Integer maxSizeLimit;
    private String protectionPeriod;
    private String dailyLimit;
    private LegalStatus legalStatus;
    private String legalStatusDisplayName;
    private Boolean isProtectedNow;

    // Biologia i taktyka w pigułce
    private WaterZone waterZone;
    private String waterZoneDisplayName;
    private DifficultyLevel difficulty;
    private String difficultyDisplayName;
    private Double recordWeightKg;
    private Double recordLengthCm;
    private List<Integer> activeMonths;

    // Sprzęt i bezpieczeństwo w pigułce
    private Boolean leaderRequired;
    private String recommendedCastingWeight;
    private String recommendedHookSize;
    private Boolean hasSafetyAlerts;
    private String safetyAlertsRaw;
}
