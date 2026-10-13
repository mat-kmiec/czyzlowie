package pl.matkmiec.backend.fish.dto;

import lombok.*;
import pl.matkmiec.backend.fish.model.DifficultyLevel;
import pl.matkmiec.backend.fish.model.FishType;
import pl.matkmiec.backend.fish.model.LegalStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FishSpeciesDetailsDto {
    private Long id;
    private String slug;
    private String name;
    private String latinName;
    private String family;
    private FishType fishType;
    private String fishTypeDisplayName;
    private String fishTypeDescription;
    private String imageUrl;
    private String thumbnailUrl;
    private String shortDescription;
    private LegalStatus legalStatus;
    private String legalStatusDisplayName;
    private DifficultyLevel difficulty;
    private String difficultyDisplayName;
    private Boolean isProtectedNow;

    // Sekcje szczegółowe
    private FishBasicInfoDto basicInfo;
    private FishLegalInfoDto legalInfo;
    private FishBiologyDto biologyAndRecords;
    private FishTacticsDto tacticsAndTips;
    private FishTackleGuideDto tackleGuide;
    private FishCatchAndReleaseDto catchAndReleaseGuide;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
