package pl.matkmiec.backend.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import pl.matkmiec.backend.auth.exception.AuthExceptionHandler;
import pl.matkmiec.backend.auth.jwt.UserPrincipal;
import pl.matkmiec.backend.auth.security.AuthCookieService;
import pl.matkmiec.backend.user.dto.*;
import pl.matkmiec.backend.user.model.ProfileVisibility;
import pl.matkmiec.backend.user.model.UserRole;
import pl.matkmiec.backend.user.model.UserStatus;
import pl.matkmiec.backend.user.service.UserService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private UserService userService;

    @Mock
    private AuthCookieService authCookieService;

    @InjectMocks
    private UserController userController;

    private UUID userId;
    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        principal = UserPrincipal.builder()
                .id(userId)
                .email("user@example.com")
                .nickname("Wedkarz_Jan")
                .role(UserRole.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                .build();

        HandlerMethodArgumentResolver authPrincipalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterType().isAssignableFrom(UserPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return principal;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setCustomArgumentResolvers(authPrincipalResolver)
                .setControllerAdvice(new AuthExceptionHandler())
                .build();
    }

    @Test
    void shouldGetCurrentUserProfile() throws Exception {
        UserProfileDto profile = UserProfileDto.builder()
                .id(userId)
                .email("user@example.com")
                .nickname("Wedkarz_Jan")
                .role(UserRole.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .settings(UserSettingsDto.builder()
                        .hideExactGpsCoordinates(true)
                        .pushNotificationsEnabled(true)
                        .profileVisibility(ProfileVisibility.PUBLIC)
                        .build())
                .build();

        when(userService.getProfile(userId)).thenReturn(profile);

        mockMvc.perform(get("/v1/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.nickname").value("Wedkarz_Jan"))
                .andExpect(jsonPath("$.settings.hideExactGpsCoordinates").value(true));
    }

    @Test
    void shouldUpdateUserProfile() throws Exception {
        UpdateProfileRequestDto request = UpdateProfileRequestDto.builder()
                .nickname("Wedkarz_NowyNick")
                .avatarUrl("https://example.com/avatar.png")
                .build();

        UserProfileDto updated = UserProfileDto.builder()
                .id(userId)
                .nickname("Wedkarz_NowyNick")
                .avatarUrl("https://example.com/avatar.png")
                .build();

        when(userService.updateProfile(eq(userId), any(UpdateProfileRequestDto.class))).thenReturn(updated);

        mockMvc.perform(put("/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("Wedkarz_NowyNick"));
    }

    @Test
    void shouldUpdateUserSettings() throws Exception {
        UserSettingsDto request = UserSettingsDto.builder()
                .pushNotificationsEnabled(false)
                .hideExactGpsCoordinates(false)
                .profileVisibility(ProfileVisibility.PRIVATE)
                .build();

        when(userService.updateSettings(eq(userId), any(UserSettingsDto.class))).thenReturn(request);

        mockMvc.perform(put("/v1/users/me/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pushNotificationsEnabled").value(false))
                .andExpect(jsonPath("$.hideExactGpsCoordinates").value(false));
    }

    @Test
    void shouldChangePassword() throws Exception {
        ChangePasswordRequestDto request = ChangePasswordRequestDto.builder()
                .currentPassword("stareHaslo123")
                .newPassword("noweHaslo456")
                .build();

        mockMvc.perform(post("/v1/users/me/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        verify(userService).changePassword(eq(userId), any(ChangePasswordRequestDto.class));
    }

    @Test
    void shouldDeleteAccountAccordingToGdpr() throws Exception {
        mockMvc.perform(delete("/v1/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        verify(userService).softDeleteUserAndAnonymize(userId);
    }

    @Test
    void shouldExportUserDataAccordingToGdpr() throws Exception {
        GdprDataExportDto exportDto = GdprDataExportDto.builder()
                .userId(userId)
                .email("user@example.com")
                .nickname("Wedkarz_Jan")
                .role("ROLE_USER")
                .consentsAndPrivacy(Map.of("hideExactGpsCoordinates", true))
                .build();

        when(userService.exportUserData(userId)).thenReturn(exportDto);

        mockMvc.perform(get("/v1/users/me/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"czyzlowie_gdpr_export_" + userId + ".json\""))
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.email").value("user@example.com"));
    }
}
