package pl.matkmiec.backend.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import pl.matkmiec.backend.auth.config.SecurityProperties;
import pl.matkmiec.backend.auth.dto.*;
import pl.matkmiec.backend.auth.exception.EmailAlreadyExistsException;
import pl.matkmiec.backend.auth.exception.InvalidCredentialsException;
import pl.matkmiec.backend.auth.jwt.JwtTokenProvider;
import pl.matkmiec.backend.auth.model.PinType;
import pl.matkmiec.backend.auth.model.RefreshToken;
import pl.matkmiec.backend.auth.repository.RefreshTokenRepository;
import pl.matkmiec.backend.user.mapper.UserMapper;
import pl.matkmiec.backend.user.model.AuthProvider;
import pl.matkmiec.backend.user.model.User;
import pl.matkmiec.backend.user.model.UserRole;
import pl.matkmiec.backend.user.model.UserStatus;
import pl.matkmiec.backend.user.repository.UserRepository;
import pl.matkmiec.backend.user.repository.UserSettingsRepository;
import pl.matkmiec.backend.user.service.NicknameGeneratorService;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSettingsRepository userSettingsRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PinCodeService pinCodeService;

    @Mock
    private EmailNotificationService emailNotificationService;

    @Mock
    private NicknameGeneratorService nicknameGeneratorService;

    @Mock
    private SocialAuthVerifierService socialAuthVerifierService;

    @Mock
    private PasswordEncoder passwordEncoder;

    private JwtTokenProvider jwtTokenProvider;
    private UserMapper userMapper;
    private SecurityProperties securityProperties;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        securityProperties = new SecurityProperties();
        securityProperties.getJwt().setSecret("verySecretKeyForTestingAuthServiceWhichIsLongEnoughForSha256HMAC!");
        securityProperties.getJwt().setAccessTokenExpirationMs(3600000);
        securityProperties.getJwt().setRefreshTokenExpirationMs(7200000);

        jwtTokenProvider = new JwtTokenProvider(securityProperties);
        userMapper = new UserMapper();

        authService = new AuthService(
                userRepository,
                userSettingsRepository,
                refreshTokenRepository,
                pinCodeService,
                emailNotificationService,
                nicknameGeneratorService,
                socialAuthVerifierService,
                jwtTokenProvider,
                passwordEncoder,
                userMapper,
                securityProperties
        );
    }

    @Test
    void shouldRegisterNewUserAndSendVerificationPin() {
        RegisterRequestDto request = RegisterRequestDto.builder()
                .email("nowy@example.com")
                .password("tajneHaslo123")
                .nickname("Wedkarz_Nowy")
                .build();

        when(userRepository.existsByEmailIgnoreCase("nowy@example.com")).thenReturn(false);
        when(userRepository.existsByNicknameIgnoreCase("Wedkarz_Nowy")).thenReturn(false);
        when(passwordEncoder.encode("tajneHaslo123")).thenReturn("encodedSecret");
        when(pinCodeService.generateAndSavePin("nowy@example.com", PinType.EMAIL_VERIFICATION)).thenReturn("1234");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        AuthResponseDto response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getRefreshToken()).isNotBlank();
        assertThat(response.getUser().getEmail()).isEqualTo("nowy@example.com");

        verify(emailNotificationService).sendVerificationPin(eq("nowy@example.com"), eq("1234"), anyInt());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void shouldThrowWhenRegisteringExistingEmail() {
        RegisterRequestDto request = RegisterRequestDto.builder()
                .email("istniejacy@example.com")
                .password("tajneHaslo123")
                .build();

        when(userRepository.existsByEmailIgnoreCase("istniejacy@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    void shouldVerifyEmailWithCorrectPin() {
        VerifyEmailRequestDto request = VerifyEmailRequestDto.builder()
                .email("user@example.com")
                .pin("1234")
                .build();

        User user = User.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .nickname("Wedkarz_1")
                .role(UserRole.ROLE_USER)
                .status(UserStatus.PENDING_VERIFICATION)
                .emailVerified(false)
                .build();

        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthResponseDto response = authService.verifyEmail(request);

        assertThat(response).isNotNull();
        assertThat(user.getEmailVerified()).isTrue();
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        verify(pinCodeService).verifyPin("user@example.com", "1234", PinType.EMAIL_VERIFICATION);
    }

    @Test
    void shouldLoginSuccessfullyWithValidCredentials() {
        LoginRequestDto request = LoginRequestDto.builder()
                .email("jan@example.com")
                .password("haslo123")
                .build();

        User user = User.builder()
                .id(UUID.randomUUID())
                .email("jan@example.com")
                .passwordHash("hashedPass")
                .nickname("Jan_Wedkarz")
                .role(UserRole.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .build();

        when(userRepository.findByEmailIgnoreCase("jan@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("haslo123", "hashedPass")).thenReturn(true);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthResponseDto response = authService.login(request, "127.0.0.1", "Mozilla/5.0");

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getUser().getEmail()).isEqualTo("jan@example.com");
    }

    @Test
    void shouldThrowInvalidCredentialsOnWrongPassword() {
        LoginRequestDto request = LoginRequestDto.builder()
                .email("jan@example.com")
                .password("zleHaslo")
                .build();

        User user = User.builder()
                .id(UUID.randomUUID())
                .email("jan@example.com")
                .passwordHash("hashedPass")
                .nickname("Jan_Wedkarz")
                .role(UserRole.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .build();

        when(userRepository.findByEmailIgnoreCase("jan@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("zleHaslo", "hashedPass")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request, "127.0.0.1", "Mozilla/5.0"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void shouldLinkAccountOnSocialLoginWhenUserWithSameEmailExists() {
        SocialLoginRequestDto request = SocialLoginRequestDto.builder()
                .provider(AuthProvider.GOOGLE)
                .idToken("dummy-token")
                .build();

        SocialAuthVerifierService.SocialUserInfo socialInfo = SocialAuthVerifierService.SocialUserInfo.builder()
                .providerId("google-id-12345")
                .email("jan@example.com")
                .name("Jan Kowalski")
                .provider(AuthProvider.GOOGLE)
                .build();

        User existingUser = User.builder()
                .id(UUID.randomUUID())
                .email("jan@example.com")
                .nickname("Wedkarz_Jan")
                .role(UserRole.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .authProvider(AuthProvider.LOCAL)
                .emailVerified(false)
                .build();

        when(socialAuthVerifierService.verifySocialLogin(request)).thenReturn(socialInfo);
        when(userRepository.findByGoogleId("google-id-12345")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("jan@example.com")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthResponseDto response = authService.socialLogin(request, "127.0.0.1", "Android App");

        assertThat(response).isNotNull();
        assertThat(existingUser.getGoogleId()).isEqualTo("google-id-12345");
        assertThat(existingUser.getEmailVerified()).isTrue();
    }

    @Test
    void shouldCheckEmailAvailabilityCorrectly() {
        when(userRepository.existsByEmailIgnoreCase("free@example.com")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("taken@example.com")).thenReturn(true);

        CheckAvailabilityResponseDto free = authService.checkEmailAvailability("free@example.com");
        CheckAvailabilityResponseDto taken = authService.checkEmailAvailability("taken@example.com");

        assertThat(free.isAvailable()).isTrue();
        assertThat(taken.isAvailable()).isFalse();
    }
}
