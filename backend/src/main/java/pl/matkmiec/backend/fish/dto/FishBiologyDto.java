package pl.matkmiec.backend.fish.dto;

import lombok.*;
import pl.matkmiec.backend.fish.model.WaterZone;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FishBiologyDto {
    private FishRecordDto record;
    private String averageWeight;
    private String averageLength;
    private List<String> waterPreferences;
    private WaterZone waterZone;
    private String waterZoneDisplayName;
    private String waterZoneDescription;
    private String naturalFood;
    private String spawningMonths;
    private String similarSpecies;
}
