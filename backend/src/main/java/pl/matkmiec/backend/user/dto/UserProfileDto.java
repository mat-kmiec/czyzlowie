package pl.matkmiec.backend.user.dto;

import lombok.*;
import pl.matkmiec.backend.user.model.AuthProvider;
import pl.matkmiec.backend.user.model.UserRole;
import pl.matkmiec.backend.user.model.UserStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileDto {
    private UUID id;
    private String email;
    private String nickname;
    private String avatarUrl;
    private UserRole role;
    private String roleDisplayName;
    private UserStatus status;
    private AuthProvider authProvider;
    private Boolean emailVerified;
    private UserSettingsDto settings;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
}
