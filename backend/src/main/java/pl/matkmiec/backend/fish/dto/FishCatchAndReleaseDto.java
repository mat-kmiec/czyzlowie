package pl.matkmiec.backend.fish.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FishCatchAndReleaseDto {
    private String landingMethod;
    private List<String> requiredGear;
    private String requiredGearRaw;
    private List<String> safetyAlerts;
    private String safetyAlertsRaw;
    private Boolean hasSafetyAlerts;
    private String specialCareTips;
    private Boolean barblessHooksRecommended;
}
