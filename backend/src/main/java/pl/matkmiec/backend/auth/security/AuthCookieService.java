package pl.matkmiec.backend.auth.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import pl.matkmiec.backend.auth.config.SecurityProperties;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthCookieService {

    public static final String ACCESS_TOKEN_COOKIE_NAME = "access_token";
    public static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";

    private final SecurityProperties securityProperties;

    public void addAuthCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        long accessExpirySeconds = securityProperties.getJwt().getAccessTokenExpirationMs() / 1000;
        long refreshExpirySeconds = securityProperties.getJwt().getRefreshTokenExpirationMs() / 1000;

        ResponseCookie accessCookie = createCookie(
                ACCESS_TOKEN_COOKIE_NAME,
                accessToken,
                accessExpirySeconds,
                "/"
        );

        ResponseCookie refreshCookie = createCookie(
                REFRESH_TOKEN_COOKIE_NAME,
                refreshToken,
                refreshExpirySeconds,
                "/api/v1/auth"
        );

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }

    public void clearAuthCookies(HttpServletResponse response) {
        ResponseCookie clearAccessCookie = createCookie(
                ACCESS_TOKEN_COOKIE_NAME,
                "",
                0,
                "/"
        );

        ResponseCookie clearRefreshCookie = createCookie(
                REFRESH_TOKEN_COOKIE_NAME,
                "",
                0,
                "/api/v1/auth"
        );

        response.addHeader(HttpHeaders.SET_COOKIE, clearAccessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, clearRefreshCookie.toString());
    }

    public Optional<String> extractTokenFromCookie(HttpServletRequest request, String cookieName) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }

        return Arrays.stream(request.getCookies())
                .filter(c -> cookieName.equals(c.getName()))
                .map(Cookie::getValue)
                .filter(v -> v != null && !v.isBlank())
                .findFirst();
    }

    private ResponseCookie createCookie(String name, String value, long maxAgeSeconds, String path) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(securityProperties.getJwt().isCookieSecure())
                .sameSite(securityProperties.getJwt().getCookieSameSite())
                .path(path)
                .maxAge(Duration.ofSeconds(maxAgeSeconds));

        String domain = securityProperties.getJwt().getCookieDomain();
        if (domain != null && !domain.isBlank()) {
            builder.domain(domain);
        }

        return builder.build();
    }
}
