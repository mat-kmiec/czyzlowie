package pl.matkmiec.backend.fish.dto;

import lombok.*;
import pl.matkmiec.backend.fish.model.FishType;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FishSafetyAlertDto {
    private Long id;
    private String slug;
    private String name;
    private String latinName;
    private String family;
    private FishType fishType;
    private String fishTypeDisplayName;
    private String thumbnailUrl;
    private Boolean hasSafetyAlerts;
    private List<String> safetyAlerts;
    private String safetyAlertsRaw;
    private String landingMethod;
    private List<String> requiredGear;
    private String requiredGearRaw;
    private String specialCareTips;
}
