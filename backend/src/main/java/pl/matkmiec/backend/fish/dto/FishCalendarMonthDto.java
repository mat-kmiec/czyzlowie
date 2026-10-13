package pl.matkmiec.backend.fish.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FishCalendarMonthDto {
    private Integer monthNumber;
    private String monthName;
    private String season;
    private String generalTips;
    private List<FishSpeciesListItemDto> activeSpecies;
    private List<FishSpeciesListItemDto> protectedSpecies;
}
