package pl.matkmiec.backend.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.matkmiec.backend.auth.exception.AuthException;
import pl.matkmiec.backend.auth.exception.NicknameAlreadyExistsException;
import pl.matkmiec.backend.auth.repository.RefreshTokenRepository;
import pl.matkmiec.backend.user.dto.*;
import pl.matkmiec.backend.user.exception.UserNotFoundException;
import pl.matkmiec.backend.user.mapper.UserMapper;
import pl.matkmiec.backend.user.model.User;
import pl.matkmiec.backend.user.model.UserStatus;
import pl.matkmiec.backend.user.repository.UserRepository;
import pl.matkmiec.backend.user.repository.UserSettingsRepository;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserProfileDto getProfile(UUID userId) {
        User user = getUserEntity(userId);
        return userMapper.toProfileDto(user);
    }

    @Transactional
    public UserProfileDto updateProfile(UUID userId, UpdateProfileRequestDto request) {
        User user = getUserEntity(userId);

        if (request.getNickname() != null && !request.getNickname().isBlank()) {
            String newNickname = request.getNickname().trim();
            if (!newNickname.equalsIgnoreCase(user.getNickname())) {
                if (userRepository.existsByNicknameIgnoreCase(newNickname)) {
                    throw new NicknameAlreadyExistsException("Nazwa użytkownika '" + newNickname + "' jest już zajęta.");
                }
                user.setNickname(newNickname);
            }
        }

        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl().trim());
        }

        User saved = userRepository.save(user);
        log.info("Zaktualizowano profil użytkownika ID: {}", userId);
        return userMapper.toProfileDto(saved);
    }

    @Transactional
    public UserProfileDto updateAvatar(UUID userId, String avatarUrl) {
        User user = getUserEntity(userId);
        user.setAvatarUrl(avatarUrl != null ? avatarUrl.trim() : null);
        User saved = userRepository.save(user);
        return userMapper.toProfileDto(saved);
    }

    @Transactional(readOnly = true)
    public UserSettingsDto getSettings(UUID userId) {
        User user = getUserEntity(userId);
        return userMapper.toSettingsDto(user.getSettings());
    }

    @Transactional
    public UserSettingsDto updateSettings(UUID userId, UserSettingsDto settingsDto) {
        User user = getUserEntity(userId);
        userMapper.updateSettingsFromDto(user.getSettings(), settingsDto);
        userSettingsRepository.save(user.getSettings());
        log.info("Zaktualizowano ustawienia użytkownika ID: {}", userId);
        return userMapper.toSettingsDto(user.getSettings());
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequestDto request) {
        User user = getUserEntity(userId);

        if (user.getPasswordHash() != null) {
            if (request.getCurrentPassword() == null ||
                    !passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
                throw new AuthException("Aktualne hasło jest niepoprawne.");
            }
        }

        if (request.getNewPassword() == null || request.getNewPassword().length() < 8) {
            throw new AuthException("Nowe hasło musi zawierać minimum 8 znaków.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Wyloguj ze wszystkich pozostałych sesji / unieważnij tokeny
        refreshTokenRepository.revokeAllByUserId(userId);
        log.info("Zmieniono hasło i unieważniono sesje dla użytkownika ID: {}", userId);
    }

    @Transactional
    public void softDeleteUserAndAnonymize(UUID userId) {
        User user = getUserEntity(userId);

        log.info("Rozpoczynam procedurę anonimizacji i usunięcia konta RODO (Right to be Forgotten) dla ID: {}", userId);

        // Złota zasada danych: usuwamy email, hasło, nick, avatar, odpinamy powiązania
        user.setEmail(null);
        user.setPasswordHash(null);
        user.setNickname("Usunięty wędkarz (" + userId.toString().substring(0, 8) + ")");
        user.setAvatarUrl(null);
        user.setGoogleId(null);
        user.setAppleId(null);
        user.setStatus(UserStatus.DELETED);
        user.setIsGhostUser(true);
        user.setDeletedAt(LocalDateTime.now());

        // Wyczyść wszystkie aktywne tokeny sesyjne
        refreshTokenRepository.deleteAllByUserId(userId);

        userRepository.save(user);
        log.info("Konto użytkownika ID: {} zostało pomyślnie zanonimizowane i oznaczone jako usunięte.", userId);
    }

    @Transactional(readOnly = true)
    public GdprDataExportDto exportUserData(UUID userId) {
        User user = getUserEntity(userId);

        Map<String, Object> consents = Map.of(
                "pushNotifications", user.getSettings() != null && Boolean.TRUE.equals(user.getSettings().getPushNotificationsEnabled()),
                "webPushNotifications", user.getSettings() != null && Boolean.TRUE.equals(user.getSettings().getWebPushEnabled()),
                "emailNotifications", user.getSettings() != null && Boolean.TRUE.equals(user.getSettings().getEmailNotificationsEnabled()),
                "hideExactGpsCoordinates", user.getSettings() != null && Boolean.TRUE.equals(user.getSettings().getHideExactGpsCoordinates()),
                "profileVisibility", user.getSettings() != null ? user.getSettings().getProfileVisibility().name() : "PUBLIC"
        );

        Map<String, Object> meta = Map.of(
                "gdprArticle", "Article 20 GDPR (Right to data portability)",
                "platform", "CzyZłowie Fishing Platform",
                "exportFormat", "application/json"
        );

        return GdprDataExportDto.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .role(user.getRole().name())
                .authProvider(user.getAuthProvider().name())
                .accountCreatedAt(user.getCreatedAt())
                .exportGeneratedAt(LocalDateTime.now())
                .exportVersion("1.0.0")
                .settings(userMapper.toSettingsDto(user.getSettings()))
                .consentsAndPrivacy(consents)
                .activeSessions(List.of())
                .fishingDiaryCatches(List.of())
                .metadata(meta)
                .build();
    }

    public User getUserEntity(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }
}
