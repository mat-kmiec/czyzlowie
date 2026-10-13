package pl.matkmiec.backend.fish.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FishTackleGuideDto {
    private String lineMonofilament;
    private String lineBraid;
    private String recommendedHookSize;
    private String recommendedCastingWeight;
    private String recommendedRodAction;
    private Boolean leaderRequired;
    private String recommendedLeader;
    private String additionalGearTips;
}
