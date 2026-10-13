package pl.matkmiec.backend.auth.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.matkmiec.backend.auth.config.SecurityProperties;
import pl.matkmiec.backend.user.model.AuthProvider;
import pl.matkmiec.backend.user.model.User;
import pl.matkmiec.backend.user.model.UserRole;
import pl.matkmiec.backend.user.model.UserStatus;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        SecurityProperties securityProperties = new SecurityProperties();
        securityProperties.getJwt().setSecret("verySecretKeyForTestingJwtTokenProviderWhichIsLongEnoughForSha256HMAC!");
        securityProperties.getJwt().setAccessTokenExpirationMs(3600000);
        securityProperties.getJwt().setRefreshTokenExpirationMs(7200000);

        jwtTokenProvider = new JwtTokenProvider(securityProperties);
    }

    @Test
    void shouldGenerateAndValidateTokenSuccessfully() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email("jan.kowalski@example.com")
                .nickname("Wedkarz_Jan")
                .role(UserRole.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .authProvider(AuthProvider.LOCAL)
                .emailVerified(true)
                .build();

        String token = jwtTokenProvider.generateAccessToken(user);

        assertThat(token).isNotBlank();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getUserIdFromToken(token)).isEqualTo(userId);
        assertThat(jwtTokenProvider.getEmailFromToken(token)).isEqualTo("jan.kowalski@example.com");
    }

    @Test
    void shouldReturnFalseForInvalidOrTamperedToken() {
        String invalidToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.invalid.signature";
        assertThat(jwtTokenProvider.validateToken(invalidToken)).isFalse();
    }

    @Test
    void shouldGenerateUniqueRefreshTokensAndHashes() {
        String token1 = jwtTokenProvider.generateRefreshTokenString();
        String token2 = jwtTokenProvider.generateRefreshTokenString();

        assertThat(token1).isNotBlank();
        assertThat(token2).isNotBlank();
        assertThat(token1).isNotEqualTo(token2);

        String hash1 = jwtTokenProvider.hashToken(token1);
        String hash2 = jwtTokenProvider.hashToken(token1);
        assertThat(hash1).isEqualTo(hash2);
    }
}
