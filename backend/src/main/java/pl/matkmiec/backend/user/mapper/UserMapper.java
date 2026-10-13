package pl.matkmiec.backend.user.mapper;

import org.springframework.stereotype.Component;
import pl.matkmiec.backend.user.dto.UserProfileDto;
import pl.matkmiec.backend.user.dto.UserSettingsDto;
import pl.matkmiec.backend.user.model.User;
import pl.matkmiec.backend.user.model.UserRole;
import pl.matkmiec.backend.user.model.UserSettings;

@Component
public class UserMapper {

    public UserProfileDto toProfileDto(User user) {
        if (user == null) {
            return null;
        }

        UserSettingsDto settingsDto = toSettingsDto(user.getSettings());

        return UserProfileDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .roleDisplayName(formatRoleDisplayName(user.getRole()))
                .status(user.getStatus())
                .authProvider(user.getAuthProvider())
                .emailVerified(user.getEmailVerified())
                .settings(settingsDto)
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }

    public UserSettingsDto toSettingsDto(UserSettings settings) {
        if (settings == null) {
            return null;
        }

        return UserSettingsDto.builder()
                .pushNotificationsEnabled(settings.getPushNotificationsEnabled())
                .webPushEnabled(settings.getWebPushEnabled())
                .emailNotificationsEnabled(settings.getEmailNotificationsEnabled())
                .hideExactGpsCoordinates(settings.getHideExactGpsCoordinates())
                .profileVisibility(settings.getProfileVisibility())
                .build();
    }

    public void updateSettingsFromDto(UserSettings settings, UserSettingsDto dto) {
        if (settings == null || dto == null) {
            return;
        }

        if (dto.getPushNotificationsEnabled() != null) {
            settings.setPushNotificationsEnabled(dto.getPushNotificationsEnabled());
        }
        if (dto.getWebPushEnabled() != null) {
            settings.setWebPushEnabled(dto.getWebPushEnabled());
        }
        if (dto.getEmailNotificationsEnabled() != null) {
            settings.setEmailNotificationsEnabled(dto.getEmailNotificationsEnabled());
        }
        if (dto.getHideExactGpsCoordinates() != null) {
            settings.setHideExactGpsCoordinates(dto.getHideExactGpsCoordinates());
        }
        if (dto.getProfileVisibility() != null) {
            settings.setProfileVisibility(dto.getProfileVisibility());
        }
    }

    private String formatRoleDisplayName(UserRole role) {
        if (role == null) return "Użytkownik";
        return switch (role) {
            case ROLE_ADMIN -> "Administrator";
            case ROLE_MODERATOR_OKREGU -> "Moderator Okręgu";
            case ROLE_USER -> "Wędkarz";
        };
    }
}
