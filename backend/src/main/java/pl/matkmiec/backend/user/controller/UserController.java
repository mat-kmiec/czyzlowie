package pl.matkmiec.backend.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pl.matkmiec.backend.auth.jwt.UserPrincipal;
import pl.matkmiec.backend.auth.security.AuthCookieService;
import pl.matkmiec.backend.user.dto.*;
import pl.matkmiec.backend.user.service.UserService;

import java.util.Map;

@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Profile & GDPR", description = "User profile management, cloud-synced preferences, password changes, GDPR data export and account anonymization/deletion.")
public class UserController {

    private final UserService userService;
    private final AuthCookieService authCookieService;

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile",
            description = "Returns current user details, role, status, and cloud-synchronized settings.")
    public ResponseEntity<UserProfileDto> getCurrentProfile(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(userService.getProfile(principal.getId()));
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user profile",
            description = "Updates nickname and avatar URL.")
    public ResponseEntity<UserProfileDto> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody UpdateProfileRequestDto request
    ) {
        return ResponseEntity.ok(userService.updateProfile(principal.getId(), request));
    }

    @PatchMapping("/me")
    @Operation(summary = "Partially update current user profile",
            description = "Alias for updating profile fields.")
    public ResponseEntity<UserProfileDto> patchProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody UpdateProfileRequestDto request
    ) {
        return ResponseEntity.ok(userService.updateProfile(principal.getId(), request));
    }

    @PostMapping("/me/avatar")
    @Operation(summary = "Update user avatar image URL",
            description = "Sets avatar URL from camera roll upload or direct URL.")
    public ResponseEntity<UserProfileDto> updateAvatar(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, String> request
    ) {
        String avatarUrl = request.get("avatarUrl");
        return ResponseEntity.ok(userService.updateAvatar(principal.getId(), avatarUrl));
    }

    @GetMapping("/me/settings")
    @Operation(summary = "Get user settings and privacy preferences",
            description = "Returns cloud-synced preferences: Push/WebPush/Email notifications and GPS coordinate obfuscation.")
    public ResponseEntity<UserSettingsDto> getSettings(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(userService.getSettings(principal.getId()));
    }

    @PutMapping("/me/settings")
    @Operation(summary = "Update user settings and privacy preferences",
            description = "Updates notification consents and privacy settings across all devices.")
    public ResponseEntity<UserSettingsDto> updateSettings(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody UserSettingsDto settingsDto
    ) {
        return ResponseEntity.ok(userService.updateSettings(principal.getId(), settingsDto));
    }

    @PostMapping("/me/change-password")
    @Operation(summary = "Change password",
            description = "Validates current password, sets new password, and invalidates other active device sessions.")
    public ResponseEntity<Map<String, String>> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody ChangePasswordRequestDto request
    ) {
        userService.changePassword(principal.getId(), request);
        return ResponseEntity.ok(Map.of("message", "Hasło zostało pomyślnie zmienione. Pozostałe aktywne sesje zostały zakończone."));
    }

    @DeleteMapping("/me")
    @Operation(summary = "Delete user account (GDPR Soft Delete & Anonymization)",
            description = "App Store / Google Play compliant account deletion. Clears all PII (email, nickname, avatar, GPS logs) while retaining anonymized ecological/weather logs under Ghost User.")
    public ResponseEntity<Map<String, String>> deleteAccount(
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletResponse response
    ) {
        userService.softDeleteUserAndAnonymize(principal.getId());
        authCookieService.clearAuthCookies(response);
        return ResponseEntity.ok(Map.of("message", "Konto zostało pomyślnie usunięte i zanonimizowane zgodnie z przepisami RODO / GDPR."));
    }

    @GetMapping("/me/export")
    @Operation(summary = "Export all user data (GDPR Right to Data Portability)",
            description = "Generates a full JSON data package containing profile details, settings, consents, and diary entries.")
    public ResponseEntity<GdprDataExportDto> exportData(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        GdprDataExportDto exportDto = userService.exportUserData(principal.getId());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"czyzlowie_gdpr_export_" + principal.getId() + ".json\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(exportDto);
    }
}
