package pl.matkmiec.backend.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pl.matkmiec.backend.auth.dto.*;
import pl.matkmiec.backend.auth.exception.AuthExceptionHandler;
import pl.matkmiec.backend.auth.security.AuthCookieService;
import pl.matkmiec.backend.auth.service.AuthService;
import pl.matkmiec.backend.user.dto.UserProfileDto;
import pl.matkmiec.backend.user.model.UserRole;
import pl.matkmiec.backend.user.model.UserStatus;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AuthService authService;

    @Mock
    private AuthCookieService authCookieService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new AuthExceptionHandler())
                .build();
    }

    @Test
    void shouldRegisterUser() throws Exception {
        RegisterRequestDto request = RegisterRequestDto.builder()
                .email("test@example.com")
                .password("tajneHaslo123")
                .nickname("Wedkarz_Test")
                .build();

        AuthResponseDto response = AuthResponseDto.builder()
                .accessToken("access.token.jwt")
                .refreshToken("refresh-token-xyz")
                .tokenType("Bearer")
                .expiresIn(1800L)
                .user(UserProfileDto.builder()
                        .id(UUID.randomUUID())
                        .email("test@example.com")
                        .nickname("Wedkarz_Test")
                        .role(UserRole.ROLE_USER)
                        .status(UserStatus.PENDING_VERIFICATION)
                        .build())
                .build();

        when(authService.register(any(RegisterRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access.token.jwt"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token-xyz"))
                .andExpect(jsonPath("$.user.email").value("test@example.com"))
                .andExpect(jsonPath("$.user.status").value("PENDING_VERIFICATION"));
    }

    @Test
    void shouldLoginUser() throws Exception {
        LoginRequestDto request = LoginRequestDto.builder()
                .email("test@example.com")
                .password("haslo123")
                .build();

        AuthResponseDto response = AuthResponseDto.builder()
                .accessToken("access.token.jwt")
                .refreshToken("refresh-token-xyz")
                .tokenType("Bearer")
                .expiresIn(1800L)
                .user(UserProfileDto.builder()
                        .id(UUID.randomUUID())
                        .email("test@example.com")
                        .nickname("Wedkarz_Test")
                        .role(UserRole.ROLE_USER)
                        .status(UserStatus.ACTIVE)
                        .build())
                .build();

        when(authService.login(any(LoginRequestDto.class), any(), any())).thenReturn(response);

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access.token.jwt"))
                .andExpect(jsonPath("$.user.email").value("test@example.com"));
    }

    @Test
    void shouldVerifyEmailWithPin() throws Exception {
        VerifyEmailRequestDto request = VerifyEmailRequestDto.builder()
                .email("test@example.com")
                .pin("1234")
                .build();

        AuthResponseDto response = AuthResponseDto.builder()
                .accessToken("access.token.jwt")
                .refreshToken("refresh-token-xyz")
                .user(UserProfileDto.builder()
                        .email("test@example.com")
                        .status(UserStatus.ACTIVE)
                        .emailVerified(true)
                        .build())
                .build();

        when(authService.verifyEmail(any(VerifyEmailRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/v1/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.emailVerified").value(true));
    }

    @Test
    void shouldCheckEmailAvailability() throws Exception {
        when(authService.checkEmailAvailability("test@example.com"))
                .thenReturn(new CheckAvailabilityResponseDto(true, "Adres e-mail jest dostępny."));

        mockMvc.perform(get("/v1/auth/check-email")
                        .param("email", "test@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void shouldCheckNicknameAvailability() throws Exception {
        when(authService.checkNicknameAvailability("Wedkarz_7392"))
                .thenReturn(new CheckAvailabilityResponseDto(true, "Nazwa użytkownika jest dostępna."));

        mockMvc.perform(get("/v1/auth/check-nickname")
                        .param("nickname", "Wedkarz_7392"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true));
    }
}
