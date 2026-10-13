package pl.matkmiec.backend.user.dto;

import lombok.*;
import pl.matkmiec.backend.user.model.ProfileVisibility;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSettingsDto {
    private Boolean pushNotificationsEnabled;
    private Boolean webPushEnabled;
    private Boolean emailNotificationsEnabled;
    private Boolean hideExactGpsCoordinates;
    private ProfileVisibility profileVisibility;
}
