package pl.matkmiec.backend.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.matkmiec.backend.auth.config.SecurityProperties;
import pl.matkmiec.backend.auth.dto.*;
import pl.matkmiec.backend.auth.exception.*;
import pl.matkmiec.backend.auth.jwt.JwtTokenProvider;
import pl.matkmiec.backend.auth.model.AuthPinCode;
import pl.matkmiec.backend.auth.model.PinType;
import pl.matkmiec.backend.auth.model.RefreshToken;
import pl.matkmiec.backend.auth.repository.RefreshTokenRepository;
import pl.matkmiec.backend.user.mapper.UserMapper;
import pl.matkmiec.backend.user.model.*;
import pl.matkmiec.backend.user.repository.UserRepository;
import pl.matkmiec.backend.user.repository.UserSettingsRepository;
import pl.matkmiec.backend.user.service.NicknameGeneratorService;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PinCodeService pinCodeService;
    private final EmailNotificationService emailNotificationService;
    private final NicknameGeneratorService nicknameGeneratorService;
    private final SocialAuthVerifierService socialAuthVerifierService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final SecurityProperties securityProperties;

    @Transactional
    public AuthResponseDto register(RegisterRequestDto request) {
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new AuthException("Adres e-mail jest wymagany.");
        }
        String cleanEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(cleanEmail)) {
            throw new EmailAlreadyExistsException("Konto z adresem e-mail '" + cleanEmail + "' już istnieje.");
        }

        if (request.getPassword() == null || request.getPassword().length() < 8) {
            throw new AuthException("Hasło musi mieć minimum 8 znaków.");
        }

        String nickname = request.getNickname();
        if (nickname != null && !nickname.isBlank()) {
            nickname = nickname.trim();
            if (userRepository.existsByNicknameIgnoreCase(nickname)) {
                throw new NicknameAlreadyExistsException("Nazwa użytkownika '" + nickname + "' jest już zajęta.");
            }
        } else {
            String emailPrefix = cleanEmail.contains("@") ? cleanEmail.substring(0, cleanEmail.indexOf('@')) : "Wedkarz";
            nickname = nicknameGeneratorService.generateUniqueNickname(emailPrefix);
        }

        User user = User.builder()
                .email(cleanEmail)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .nickname(nickname)
                .role(UserRole.ROLE_USER)
                .status(UserStatus.PENDING_VERIFICATION)
                .authProvider(AuthProvider.LOCAL)
                .emailVerified(false)
                .isGhostUser(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .lastLoginAt(LocalDateTime.now())
                .build();

        UserSettings settings = UserSettings.builder()
                .user(user)
                .pushNotificationsEnabled(true)
                .webPushEnabled(true)
                .emailNotificationsEnabled(true)
                .hideExactGpsCoordinates(true)
                .profileVisibility(ProfileVisibility.PUBLIC)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        user.setSettings(settings);
        User savedUser = userRepository.save(user);

        // Wygeneruj 4-cyfrowy kod PIN i wyślij email
        String pinCode = pinCodeService.generateAndSavePin(cleanEmail, PinType.EMAIL_VERIFICATION);
        emailNotificationService.sendVerificationPin(
                cleanEmail,
                pinCode,
                securityProperties.getPin().getExpirationMinutes()
        );

        return createAuthResponse(savedUser, "Rejestracja", null, null);
    }

    @Transactional
    public AuthResponseDto verifyEmail(VerifyEmailRequestDto request) {
        if (request.getEmail() == null || request.getPin() == null) {
            throw new AuthException("Email i kod PIN są wymagane do weryfikacji.");
        }
        String cleanEmail = request.getEmail().trim().toLowerCase();

        pinCodeService.verifyPin(cleanEmail, request.getPin(), PinType.EMAIL_VERIFICATION);

        User user = userRepository.findByEmailIgnoreCase(cleanEmail)
                .orElseThrow(() -> new AuthException("Nie znaleziono konta dla adresu: " + cleanEmail));

        user.setEmailVerified(true);
        if (user.getStatus() == UserStatus.PENDING_VERIFICATION) {
            user.setStatus(UserStatus.ACTIVE);
        }
        user.setLastLoginAt(LocalDateTime.now());
        User savedUser = userRepository.save(user);

        log.info("Pomyślnie zweryfikowano adres e-mail dla: {}", cleanEmail);
        return createAuthResponse(savedUser, "Weryfikacja Email", null, null);
    }

    @Transactional
    public void resendPin(ResendPinRequestDto request) {
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new AuthException("Adres e-mail jest wymagany.");
        }
        String cleanEmail = request.getEmail().trim().toLowerCase();
        PinType pinType = request.getPinType() != null ? request.getPinType() : PinType.EMAIL_VERIFICATION;

        String pinCode = pinCodeService.generateAndSavePin(cleanEmail, pinType);

        if (pinType == PinType.EMAIL_VERIFICATION) {
            emailNotificationService.sendVerificationPin(cleanEmail, pinCode, securityProperties.getPin().getExpirationMinutes());
        } else if (pinType == PinType.PASSWORD_RESET) {
            emailNotificationService.sendPasswordResetPin(cleanEmail, pinCode, securityProperties.getPin().getExpirationMinutes());
        }
    }

    @Transactional
    public AuthResponseDto login(LoginRequestDto request, String clientIp, String userAgent) {
        if (request.getEmail() == null || request.getPassword() == null) {
            throw new InvalidCredentialsException("Adres e-mail i hasło są wymagane.");
        }
        String cleanEmail = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmailIgnoreCase(cleanEmail)
                .orElseThrow(() -> new InvalidCredentialsException("Nieprawidłowy adres e-mail lub hasło."));

        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new AccountInactiveException("Twoje konto zostało zawieszone. Skontaktuj się z administratorem.");
        }
        if (user.getStatus() == UserStatus.DELETED || Boolean.TRUE.equals(user.getIsGhostUser())) {
            throw new AccountInactiveException("To konto zostało usunięte.");
        }

        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Nieprawidłowy adres e-mail lub hasło.");
        }

        user.setLastLoginAt(LocalDateTime.now());
        User savedUser = userRepository.save(user);

        return createAuthResponse(savedUser, "Logowanie", clientIp, userAgent);
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequestDto request) {
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new AuthException("Adres e-mail jest wymagany.");
        }
        String cleanEmail = request.getEmail().trim().toLowerCase();

        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(cleanEmail);
        if (userOpt.isPresent() && userOpt.get().getStatus() != UserStatus.DELETED) {
            String pinCode = pinCodeService.generateAndSavePin(cleanEmail, PinType.PASSWORD_RESET);
            emailNotificationService.sendPasswordResetPin(cleanEmail, pinCode, securityProperties.getPin().getExpirationMinutes());
        } else {
            // Logujemy informacyjnie, nie zdradzając w odpowiedzi czy email istnieje (ochrona przed enumeracją kont)
            log.info("Żądanie resetu hasła dla nieistniejącego maila: {}", cleanEmail);
        }
    }

    @Transactional
    public void verifyResetPin(VerifyResetPinRequestDto request) {
        if (request.getEmail() == null || request.getPin() == null) {
            throw new AuthException("Email i kod PIN są wymagane.");
        }
        pinCodeService.verifyPin(request.getEmail().trim().toLowerCase(), request.getPin(), PinType.PASSWORD_RESET);
    }

    @Transactional
    public AuthResponseDto resetPassword(ResetPasswordRequestDto request, String clientIp, String userAgent) {
        if (request.getEmail() == null || request.getPin() == null || request.getNewPassword() == null) {
            throw new AuthException("Wszystkie pola są wymagane do resetu hasła.");
        }
        if (request.getNewPassword().length() < 8) {
            throw new AuthException("Nowe hasło musi zawierać minimum 8 znaków.");
        }

        String cleanEmail = request.getEmail().trim().toLowerCase();
        pinCodeService.verifyPin(cleanEmail, request.getPin(), PinType.PASSWORD_RESET);

        User user = userRepository.findByEmailIgnoreCase(cleanEmail)
                .orElseThrow(() -> new AuthException("Nie znaleziono konta dla adresu: " + cleanEmail));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setEmailVerified(true);
        if (user.getStatus() == UserStatus.PENDING_VERIFICATION) {
            user.setStatus(UserStatus.ACTIVE);
        }
        user.setLastLoginAt(LocalDateTime.now());
        User savedUser = userRepository.save(user);

        // Unieważnij wszystkie dotychczasowe sesje / tokeny
        refreshTokenRepository.revokeAllByUserId(savedUser.getId());

        log.info("Pomyślnie zresetowano hasło i zautoryzowano użytkownika: {}", cleanEmail);
        return createAuthResponse(savedUser, "Reset hasła", clientIp, userAgent);
    }

    @Transactional
    public AuthResponseDto refreshToken(String rawRefreshToken, String clientIp, String userAgent) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new InvalidTokenException("Brak tokenu odświeżania (refresh token).");
        }

        String tokenHash = jwtTokenProvider.hashToken(rawRefreshToken);
        RefreshToken tokenEntity = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Nieprawidłowy lub nieistniejący token odświeżania."));

        if (!tokenEntity.isValid()) {
            throw new InvalidTokenException("Token odświeżania wygasł lub został unieważniony.");
        }

        User user = tokenEntity.getUser();
        if (user.getStatus() == UserStatus.SUSPENDED || user.getStatus() == UserStatus.DELETED) {
            throw new AccountInactiveException("Konto użytkownika jest nieaktywne.");
        }

        // Rotacja tokenu: oznacz stary jako unieważniony
        tokenEntity.setRevoked(true);
        refreshTokenRepository.save(tokenEntity);

        // Wygeneruj nową parę tokenów
        return createAuthResponse(user, "Odświeżenie tokenu", clientIp, userAgent);
    }

    @Transactional
    public void logout(String rawRefreshToken, UUID userId) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            String tokenHash = jwtTokenProvider.hashToken(rawRefreshToken);
            refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(t -> {
                t.setRevoked(true);
                refreshTokenRepository.save(t);
            });
        }
        if (userId != null) {
            log.info("Wylogowano użytkownika ID: {}", userId);
        }
    }

    @Transactional
    public AuthResponseDto socialLogin(SocialLoginRequestDto request, String clientIp, String userAgent) {
        SocialAuthVerifierService.SocialUserInfo socialInfo = socialAuthVerifierService.verifySocialLogin(request);
        String email = socialInfo.getEmail();
        String providerId = socialInfo.getProviderId();
        AuthProvider provider = socialInfo.getProvider();

        // 1. Sprawdź, czy użytkownik z tym providerId już istnieje
        Optional<User> userBySocialId = provider == AuthProvider.GOOGLE
                ? userRepository.findByGoogleId(providerId)
                : userRepository.findByAppleId(providerId);

        User user;
        if (userBySocialId.isPresent()) {
            user = userBySocialId.get();
        } else {
            // 2. Account Linking: Sprawdź czy użytkownik z tym samym adresem e-mail już istnieje
            Optional<User> userByEmail = userRepository.findByEmailIgnoreCase(email);
            if (userByEmail.isPresent()) {
                user = userByEmail.get();
                // Połącz konto
                if (provider == AuthProvider.GOOGLE) {
                    user.setGoogleId(providerId);
                } else if (provider == AuthProvider.APPLE) {
                    user.setAppleId(providerId);
                }
                user.setEmailVerified(true);
                log.info("Pomyślnie połączono konto {} z istniejącym kontem e-mail: {}", provider, email);
            } else {
                // 3. Utwórz nowe konto
                String nickname = nicknameGeneratorService.generateUniqueNickname(socialInfo.getName());
                user = User.builder()
                        .email(email)
                        .nickname(nickname)
                        .avatarUrl(socialInfo.getAvatarUrl())
                        .role(UserRole.ROLE_USER)
                        .status(UserStatus.ACTIVE)
                        .authProvider(provider)
                        .googleId(provider == AuthProvider.GOOGLE ? providerId : null)
                        .appleId(provider == AuthProvider.APPLE ? providerId : null)
                        .emailVerified(true)
                        .isGhostUser(false)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .lastLoginAt(LocalDateTime.now())
                        .build();

                UserSettings settings = UserSettings.builder()
                        .user(user)
                        .pushNotificationsEnabled(true)
                        .webPushEnabled(true)
                        .emailNotificationsEnabled(true)
                        .hideExactGpsCoordinates(true)
                        .profileVisibility(ProfileVisibility.PUBLIC)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();

                user.setSettings(settings);
            }
        }

        user.setLastLoginAt(LocalDateTime.now());
        User savedUser = userRepository.save(user);

        return createAuthResponse(savedUser, "Logowanie Social (" + provider + ")", clientIp, userAgent);
    }

    @Transactional
    public AuthResponseDto biometricLogin(BiometricLoginRequestDto request, String clientIp, String userAgent) {
        if (request.getRefreshToken() == null || request.getRefreshToken().isBlank()) {
            throw new InvalidTokenException("Brak zarejestrowanego tokenu biometrycznego.");
        }
        return refreshToken(request.getRefreshToken(), clientIp, userAgent);
    }

    @Transactional(readOnly = true)
    public CheckAvailabilityResponseDto checkEmailAvailability(String email) {
        if (email == null || email.isBlank()) {
            return new CheckAvailabilityResponseDto(false, "Adres e-mail nie może być pusty.");
        }
        String cleanEmail = email.trim().toLowerCase();
        boolean exists = userRepository.existsByEmailIgnoreCase(cleanEmail);
        return new CheckAvailabilityResponseDto(
                !exists,
                exists ? "Ten adres e-mail jest już zajęty." : "Adres e-mail jest dostępny."
        );
    }

    @Transactional(readOnly = true)
    public CheckAvailabilityResponseDto checkNicknameAvailability(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            return new CheckAvailabilityResponseDto(false, "Nazwa użytkownika nie może być pusta.");
        }
        String cleanNickname = nickname.trim();
        boolean exists = userRepository.existsByNicknameIgnoreCase(cleanNickname);
        return new CheckAvailabilityResponseDto(
                !exists,
                exists ? "Ta nazwa użytkownika jest już zajęta." : "Nazwa użytkownika jest dostępna."
        );
    }

    private AuthResponseDto createAuthResponse(User user, String action, String clientIp, String userAgent) {
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String rawRefreshToken = jwtTokenProvider.generateRefreshTokenString();
        String tokenHash = jwtTokenProvider.hashToken(rawRefreshToken);

        long refreshDurationMs = jwtTokenProvider.getRefreshTokenExpirationMs();
        LocalDateTime refreshExpiresAt = LocalDateTime.now().plusSeconds(refreshDurationMs / 1000);

        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .ipAddress(clientIp)
                .deviceInfo(userAgent != null && userAgent.length() > 500 ? userAgent.substring(0, 500) : userAgent)
                .expiresAt(refreshExpiresAt)
                .revoked(false)
                .createdAt(LocalDateTime.now())
                .build();

        refreshTokenRepository.save(refreshTokenEntity);

        return AuthResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpirationMs() / 1000)
                .user(userMapper.toProfileDto(user))
                .build();
    }
}
