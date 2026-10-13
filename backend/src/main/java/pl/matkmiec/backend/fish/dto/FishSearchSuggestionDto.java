package pl.matkmiec.backend.fish.dto;

import lombok.*;
import pl.matkmiec.backend.fish.model.FishType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FishSearchSuggestionDto {
    private Long id;
    private String slug;
    private String name;
    private String latinName;
    private String family;
    private FishType fishType;
    private String fishTypeDisplayName;
    private String thumbnailUrl;
}
