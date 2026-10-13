package pl.matkmiec.backend.fish.dto;

import lombok.*;
import pl.matkmiec.backend.fish.model.DifficultyLevel;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FishTacticsDto {
    private String dailyActivity;
    private String seasonalActivity;
    private List<Integer> activeMonths;
    private String weatherPreferences;
    private List<String> effectiveMethods;
    private String bestArtificialBaits;
    private String bestNaturalBaits;
    private String biteAndFightCharacteristics;
    private String typicalSpots;
    private DifficultyLevel difficulty;
    private String difficultyDisplayName;
    private String difficultyDescription;
}
