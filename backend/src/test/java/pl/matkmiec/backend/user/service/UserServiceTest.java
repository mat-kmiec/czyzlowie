package pl.matkmiec.backend.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import pl.matkmiec.backend.auth.repository.RefreshTokenRepository;
import pl.matkmiec.backend.user.dto.*;
import pl.matkmiec.backend.user.mapper.UserMapper;
import pl.matkmiec.backend.user.model.*;
import pl.matkmiec.backend.user.repository.UserRepository;
import pl.matkmiec.backend.user.repository.UserSettingsRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSettingsRepository userSettingsRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserMapper userMapper;
    private UserService userService;

    private UUID userId;
    private User user;
    private UserSettings settings;

    @BeforeEach
    void setUp() {
        userMapper = new UserMapper();
        userService = new UserService(
                userRepository,
                userSettingsRepository,
                refreshTokenRepository,
                userMapper,
                passwordEncoder
        );

        userId = UUID.randomUUID();
        user = User.builder()
                .id(userId)
                .email("wedkarz@example.com")
                .passwordHash("hashedPass")
                .nickname("Wedkarz_Jan")
                .role(UserRole.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .authProvider(AuthProvider.LOCAL)
                .emailVerified(true)
                .isGhostUser(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        settings = UserSettings.builder()
                .userId(userId)
                .user(user)
                .pushNotificationsEnabled(true)
                .webPushEnabled(false)
                .emailNotificationsEnabled(true)
                .hideExactGpsCoordinates(true)
                .profileVisibility(ProfileVisibility.PUBLIC)
                .build();

        user.setSettings(settings);
    }

    @Test
    void shouldGetProfileSuccessfully() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserProfileDto profile = userService.getProfile(userId);

        assertThat(profile).isNotNull();
        assertThat(profile.getId()).isEqualTo(userId);
        assertThat(profile.getEmail()).isEqualTo("wedkarz@example.com");
        assertThat(profile.getNickname()).isEqualTo("Wedkarz_Jan");
        assertThat(profile.getSettings().getHideExactGpsCoordinates()).isTrue();
    }

    @Test
    void shouldUpdateProfileNicknameAndAvatar() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByNicknameIgnoreCase("Wedkarz_Mistrz")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateProfileRequestDto request = UpdateProfileRequestDto.builder()
                .nickname("Wedkarz_Mistrz")
                .avatarUrl("https://example.com/avatar.jpg")
                .build();

        UserProfileDto updated = userService.updateProfile(userId, request);

        assertThat(updated.getNickname()).isEqualTo("Wedkarz_Mistrz");
        assertThat(updated.getAvatarUrl()).isEqualTo("https://example.com/avatar.jpg");
    }

    @Test
    void shouldUpdateSettings() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userSettingsRepository.save(any(UserSettings.class))).thenAnswer(inv -> inv.getArgument(0));

        UserSettingsDto request = UserSettingsDto.builder()
                .pushNotificationsEnabled(false)
                .webPushEnabled(true)
                .emailNotificationsEnabled(false)
                .hideExactGpsCoordinates(false)
                .profileVisibility(ProfileVisibility.FRIENDS)
                .build();

        UserSettingsDto updated = userService.updateSettings(userId, request);

        assertThat(updated.getPushNotificationsEnabled()).isFalse();
        assertThat(updated.getWebPushEnabled()).isTrue();
        assertThat(updated.getHideExactGpsCoordinates()).isFalse();
        assertThat(updated.getProfileVisibility()).isEqualTo(ProfileVisibility.FRIENDS);
    }

    @Test
    void shouldSoftDeleteAndAnonymizeUserAccountAccordingToGdpr() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.softDeleteUserAndAnonymize(userId);

        assertThat(user.getEmail()).isNull();
        assertThat(user.getPasswordHash()).isNull();
        assertThat(user.getAvatarUrl()).isNull();
        assertThat(user.getStatus()).isEqualTo(UserStatus.DELETED);
        assertThat(user.getIsGhostUser()).isTrue();
        assertThat(user.getDeletedAt()).isNotNull();
        assertThat(user.getNickname()).startsWith("Usunięty wędkarz");

        verify(refreshTokenRepository).deleteAllByUserId(userId);
    }

    @Test
    void shouldExportUserDataForGdpr() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        GdprDataExportDto export = userService.exportUserData(userId);

        assertThat(export).isNotNull();
        assertThat(export.getUserId()).isEqualTo(userId);
        assertThat(export.getEmail()).isEqualTo("wedkarz@example.com");
        assertThat(export.getConsentsAndPrivacy()).containsKey("hideExactGpsCoordinates");
        assertThat(export.getConsentsAndPrivacy().get("hideExactGpsCoordinates")).isEqualTo(true);
    }
}
