package pl.matkmiec.backend.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pl.matkmiec.backend.auth.dto.*;
import pl.matkmiec.backend.auth.jwt.UserPrincipal;
import pl.matkmiec.backend.auth.security.AuthCookieService;
import pl.matkmiec.backend.auth.service.AuthService;
import pl.matkmiec.backend.user.model.AuthProvider;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping({"/v1/auth", "/public/auth"})
@RequiredArgsConstructor
@Tag(name = "Authentication & Onboarding", description = "Endpoints for user registration, traditional login, Google/Apple social login, biometric login, 4-digit PIN email verification, and password recovery.")
public class AuthController {

    private final AuthService authService;
    private final AuthCookieService authCookieService;

    @PostMapping("/register")
    @Operation(summary = "Register new user account",
            description = "Creates user account in PENDING_VERIFICATION status, generates a 4-digit verification PIN sent via email, and returns JWT auth tokens (with HttpOnly cookies for Web).")
    public ResponseEntity<AuthResponseDto> register(
            @RequestBody RegisterRequestDto request,
            HttpServletResponse response
    ) {
        AuthResponseDto authResponse = authService.register(request);
        authCookieService.addAuthCookies(response, authResponse.getAccessToken(), authResponse.getRefreshToken());
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/login")
    @Operation(summary = "Traditional login (Email + Password)",
            description = "Authenticates user with email and password. Returns JWT Access and Refresh tokens in JSON response body (for Mobile Keychain/Keystore) and sets HttpOnly Secure cookies (for Web).")
    public ResponseEntity<AuthResponseDto> login(
            @RequestBody LoginRequestDto request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        String clientIp = extractClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        AuthResponseDto authResponse = authService.login(request, clientIp, userAgent);
        authCookieService.addAuthCookies(httpResponse, authResponse.getAccessToken(), authResponse.getRefreshToken());
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/logout")
    @Operation(summary = "Log out user",
            description = "Revokes refresh token and clears HttpOnly authentication cookies.")
    public ResponseEntity<Map<String, String>> logout(
            @RequestBody(required = false) RefreshTokenRequestDto request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        String tokenToRevoke = null;
        if (request != null && request.getRefreshToken() != null) {
            tokenToRevoke = request.getRefreshToken();
        } else {
            Optional<String> cookieToken = authCookieService.extractTokenFromCookie(httpRequest, AuthCookieService.REFRESH_TOKEN_COOKIE_NAME);
            tokenToRevoke = cookieToken.orElse(null);
        }

        authService.logout(tokenToRevoke, principal != null ? principal.getId() : null);
        authCookieService.clearAuthCookies(httpResponse);

        return ResponseEntity.ok(Map.of("message", "Pomyślnie wylogowano."));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh JWT access token",
            description = "Rotates refresh token and issues fresh access token. Supports token in JSON body or from HttpOnly refresh_token cookie.")
    public ResponseEntity<AuthResponseDto> refreshToken(
            @RequestBody(required = false) RefreshTokenRequestDto request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        String rawRefreshToken = null;
        if (request != null && request.getRefreshToken() != null && !request.getRefreshToken().isBlank()) {
            rawRefreshToken = request.getRefreshToken();
        } else {
            Optional<String> cookieToken = authCookieService.extractTokenFromCookie(httpRequest, AuthCookieService.REFRESH_TOKEN_COOKIE_NAME);
            rawRefreshToken = cookieToken.orElse(null);
        }

        String clientIp = extractClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        AuthResponseDto authResponse = authService.refreshToken(rawRefreshToken, clientIp, userAgent);
        authCookieService.addAuthCookies(httpResponse, authResponse.getAccessToken(), authResponse.getRefreshToken());
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Confirm email address using 4-digit PIN",
            description = "Universal verification for Mobile and Web. Activates account upon entering correct 4-digit PIN.")
    public ResponseEntity<AuthResponseDto> verifyEmail(
            @RequestBody VerifyEmailRequestDto request,
            HttpServletResponse response
    ) {
        AuthResponseDto authResponse = authService.verifyEmail(request);
        authCookieService.addAuthCookies(response, authResponse.getAccessToken(), authResponse.getRefreshToken());
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/resend-pin")
    @Operation(summary = "Resend 4-digit PIN code",
            description = "Generates and sends fresh 4-digit PIN to user email.")
    public ResponseEntity<Map<String, String>> resendPin(
            @RequestBody ResendPinRequestDto request
    ) {
        authService.resendPin(request);
        return ResponseEntity.ok(Map.of("message", "Nowy 4-cyfrowy kod PIN został wysłany na podany adres e-mail."));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset via 4-digit PIN",
            description = "Generates a 15-minute 4-digit reset PIN and sends it to the user's email.")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @RequestBody ForgotPasswordRequestDto request
    ) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(Map.of("message", "Jeśli konto z podanym adresem e-mail istnieje, wysłano 4-cyfrowy kod PIN do resetu hasła."));
    }

    @PostMapping("/verify-reset-pin")
    @Operation(summary = "Verify password reset 4-digit PIN",
            description = "Validates PIN code before presenting new password entry screen.")
    public ResponseEntity<Map<String, Object>> verifyResetPin(
            @RequestBody VerifyResetPinRequestDto request
    ) {
        authService.verifyResetPin(request);
        return ResponseEntity.ok(Map.of("valid", true, "message", "Kod PIN jest prawidłowy."));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Set new password after PIN verification",
            description = "Updates password, revokes all other sessions, and logs user in automatically.")
    public ResponseEntity<AuthResponseDto> resetPassword(
            @RequestBody ResetPasswordRequestDto request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        String clientIp = extractClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        AuthResponseDto authResponse = authService.resetPassword(request, clientIp, userAgent);
        authCookieService.addAuthCookies(httpResponse, authResponse.getAccessToken(), authResponse.getRefreshToken());
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/social/google")
    @Operation(summary = "Sign in / Sign up with Google",
            description = "Supports Android/iOS Google Sign-In SDK tokens and Web OAuth2 Google tokens. Automatically links accounts by email.")
    public ResponseEntity<AuthResponseDto> googleLogin(
            @RequestBody SocialLoginRequestDto request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        request.setProvider(AuthProvider.GOOGLE);
        String clientIp = extractClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        AuthResponseDto authResponse = authService.socialLogin(request, clientIp, userAgent);
        authCookieService.addAuthCookies(httpResponse, authResponse.getAccessToken(), authResponse.getRefreshToken());
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/social/apple")
    @Operation(summary = "Sign in / Sign up with Apple",
            description = "Supports native iOS Sign in with Apple SDK and Web Sign in with Apple JS. Automatically links accounts by email.")
    public ResponseEntity<AuthResponseDto> appleLogin(
            @RequestBody SocialLoginRequestDto request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        request.setProvider(AuthProvider.APPLE);
        String clientIp = extractClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        AuthResponseDto authResponse = authService.socialLogin(request, clientIp, userAgent);
        authCookieService.addAuthCookies(httpResponse, authResponse.getAccessToken(), authResponse.getRefreshToken());
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/social/login")
    @Operation(summary = "Unified Social Login endpoint",
            description = "Generic social login supporting GOOGLE and APPLE providers with account linking.")
    public ResponseEntity<AuthResponseDto> genericSocialLogin(
            @RequestBody SocialLoginRequestDto request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        String clientIp = extractClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        AuthResponseDto authResponse = authService.socialLogin(request, clientIp, userAgent);
        authCookieService.addAuthCookies(httpResponse, authResponse.getAccessToken(), authResponse.getRefreshToken());
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/biometric/login")
    @Operation(summary = "Biometric / Passwordless login",
            description = "Exchanges securely stored Mobile Keychain/Keystore refresh token into an active JWT session upon FaceID/TouchID prompt.")
    public ResponseEntity<AuthResponseDto> biometricLogin(
            @RequestBody BiometricLoginRequestDto request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        String clientIp = extractClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        AuthResponseDto authResponse = authService.biometricLogin(request, clientIp, userAgent);
        authCookieService.addAuthCookies(httpResponse, authResponse.getAccessToken(), authResponse.getRefreshToken());
        return ResponseEntity.ok(authResponse);
    }

    @GetMapping("/check-email")
    @Operation(summary = "Check email availability (Debounce check in real-time)",
            description = "Used by registration forms to dynamically indicate whether an email is already registered.")
    public ResponseEntity<CheckAvailabilityResponseDto> checkEmail(
            @RequestParam String email
    ) {
        return ResponseEntity.ok(authService.checkEmailAvailability(email));
    }

    @GetMapping("/check-nickname")
    @Operation(summary = "Check nickname availability (Debounce check in real-time)",
            description = "Used by registration forms to dynamically indicate whether a nickname is already in use.")
    public ResponseEntity<CheckAvailabilityResponseDto> checkNickname(
            @RequestParam String nickname
    ) {
        return ResponseEntity.ok(authService.checkNicknameAvailability(nickname));
    }

    private String extractClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isBlank()) {
            return xfHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
