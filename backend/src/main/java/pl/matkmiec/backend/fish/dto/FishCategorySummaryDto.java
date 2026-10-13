package pl.matkmiec.backend.fish.dto;

import lombok.*;
import pl.matkmiec.backend.fish.model.FishType;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FishCategorySummaryDto {
    private FishType fishType;
    private String name;
    private String description;
    private Long speciesCount;
    private List<FishSpeciesListItemDto> topSpecies;
}
