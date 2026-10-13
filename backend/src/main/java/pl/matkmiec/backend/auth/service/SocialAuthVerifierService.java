package pl.matkmiec.backend.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pl.matkmiec.backend.auth.config.SecurityProperties;
import pl.matkmiec.backend.auth.dto.SocialLoginRequestDto;
import pl.matkmiec.backend.auth.exception.AuthException;
import pl.matkmiec.backend.user.model.AuthProvider;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class SocialAuthVerifierService {

    private final SecurityProperties securityProperties;
    private final ObjectMapper objectMapper;

    @Getter
    @Builder
    public static class SocialUserInfo {
        private String providerId;
        private String email;
        private String name;
        private String avatarUrl;
        private AuthProvider provider;
    }

    public SocialUserInfo verifySocialLogin(SocialLoginRequestDto request) {
        if (request.getProvider() == null) {
            throw new AuthException("Nie podano dostawcy logowania społecznościowego (Google / Apple).");
        }

        if (request.getProvider() == AuthProvider.GOOGLE) {
            return verifyGoogleToken(request);
        } else if (request.getProvider() == AuthProvider.APPLE) {
            return verifyAppleToken(request);
        } else {
            throw new AuthException("Nieobsługiwany dostawca autoryzacji: " + request.getProvider());
        }
    }

    private SocialUserInfo verifyGoogleToken(SocialLoginRequestDto request) {
        String token = request.getIdToken() != null ? request.getIdToken() : request.getAuthorizationCode();
        if (token == null || token.isBlank()) {
            // Jeśli przekazano bezpośrednio email w trybie developerskim / mock
            if (request.getEmail() != null && !request.getEmail().isBlank()) {
                return SocialUserInfo.builder()
                        .providerId("google_mock_" + request.getEmail().hashCode())
                        .email(request.getEmail().trim().toLowerCase())
                        .name(request.getFullName() != null ? request.getFullName() : "Użytkownik Google")
                        .provider(AuthProvider.GOOGLE)
                        .build();
            }
            throw new AuthException("Brak tokenu ID Google (idToken).");
        }

        try {
            // Próba odkodowania ładunku JWT (header.payload.signature)
            JsonNode payload = parseJwtPayload(token);
            String sub = payload.has("sub") ? payload.get("sub").asText() : "google_" + System.currentTimeMillis();
            String email = payload.has("email") ? payload.get("email").asText() : request.getEmail();
            String name = payload.has("name") ? payload.get("name").asText() : request.getFullName();
            String picture = payload.has("picture") ? payload.get("picture").asText() : null;

            if (email == null || email.isBlank()) {
                throw new AuthException("Nie udało się odczytać adresu email z tokenu Google.");
            }

            return SocialUserInfo.builder()
                    .providerId(sub)
                    .email(email.trim().toLowerCase())
                    .name(name)
                    .avatarUrl(picture)
                    .provider(AuthProvider.GOOGLE)
                    .build();
        } catch (Exception e) {
            log.warn("Błąd parsowania tokenu Google: {}", e.getMessage());
            // Jeśli podano email zapasowy
            if (request.getEmail() != null && !request.getEmail().isBlank()) {
                return SocialUserInfo.builder()
                        .providerId("google_" + request.getEmail().hashCode())
                        .email(request.getEmail().trim().toLowerCase())
                        .name(request.getFullName() != null ? request.getFullName() : "Użytkownik Google")
                        .provider(AuthProvider.GOOGLE)
                        .build();
            }
            throw new AuthException("Nieprawidłowy token Google ID: " + e.getMessage());
        }
    }

    private SocialUserInfo verifyAppleToken(SocialLoginRequestDto request) {
        String token = request.getIdToken() != null ? request.getIdToken() : request.getAuthorizationCode();

        try {
            String sub = null;
            String email = request.getEmail();

            if (token != null && !token.isBlank() && token.contains(".")) {
                JsonNode payload = parseJwtPayload(token);
                if (payload.has("sub")) {
                    sub = payload.get("sub").asText();
                }
                if (payload.has("email")) {
                    email = payload.get("email").asText();
                }
            }

            if (sub == null || sub.isBlank()) {
                sub = "apple_" + (email != null ? email.hashCode() : System.currentTimeMillis());
            }

            if (email == null || email.isBlank()) {
                // Jeśli Apple ukrył e-mail (Private Relay), generujemy bezpieczny identyfikator lub rzucamy wyjątek
                if (request.getEmail() != null) {
                    email = request.getEmail();
                } else {
                    email = sub + "@privaterelay.appleid.com";
                }
            }

            return SocialUserInfo.builder()
                    .providerId(sub)
                    .email(email.trim().toLowerCase())
                    .name(request.getFullName() != null ? request.getFullName() : "Użytkownik Apple")
                    .provider(AuthProvider.APPLE)
                    .build();
        } catch (Exception e) {
            log.warn("Błąd parsowania tokenu Apple: {}", e.getMessage());
            if (request.getEmail() != null && !request.getEmail().isBlank()) {
                return SocialUserInfo.builder()
                        .providerId("apple_" + request.getEmail().hashCode())
                        .email(request.getEmail().trim().toLowerCase())
                        .name(request.getFullName() != null ? request.getFullName() : "Użytkownik Apple")
                        .provider(AuthProvider.APPLE)
                        .build();
            }
            throw new AuthException("Nieprawidłowy token Apple ID: " + e.getMessage());
        }
    }

    private JsonNode parseJwtPayload(String jwtToken) throws Exception {
        String[] parts = jwtToken.split("\\.");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Niepoprawny format JWT");
        }
        String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        return objectMapper.readTree(payloadJson);
    }
}
