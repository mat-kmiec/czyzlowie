package pl.matkmiec.backend.fish.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FishProtectionOverviewDto {
    private String currentDate;
    private Integer currentMonth;
    private List<FishSpeciesListItemDto> currentlyProtectedSpecies;
    private List<FishSpeciesListItemDto> strictlyProtectedSpecies;
    private List<FishSpeciesListItemDto> invasiveSpeciesNoRelease;
    private List<FishSpeciesListItemDto> speciesWithSizeLimits;
}
