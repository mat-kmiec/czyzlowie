package pl.matkmiec.backend.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "app.security")
@Getter
@Setter
public class SecurityProperties {

    private Jwt jwt = new Jwt();
    private Cors cors = new Cors();
    private RateLimiting rateLimiting = new RateLimiting();
    private Pin pin = new Pin();
    private Social social = new Social();

    @Getter
    @Setter
    public static class Jwt {
        private String secret = "czyzlowieSuperSecretKeyForJwtSigningMustBeAtLeast256BitsLongForHS256Algorithm2026!";
        private long accessTokenExpirationMs = 1800000; // 30 min
        private long refreshTokenExpirationMs = 2592000000L; // 30 dni
        private String cookieDomain = "";
        private boolean cookieSecure = false;
        private String cookieSameSite = "Lax";
    }

    @Getter
    @Setter
    public static class Cors {
        private List<String> allowedOrigins = List.of("http://localhost:3000", "http://localhost:5173", "http://localhost:8080");
    }

    @Getter
    @Setter
    public static class RateLimiting {
        private boolean enabled = true;
        private int authLimitPerMinute = 10;
        private int bruteForceLimitPerMinute = 5;
    }

    @Getter
    @Setter
    public static class Pin {
        private int expirationMinutes = 15;
        private int length = 4;
    }

    @Getter
    @Setter
    public static class Social {
        private Google google = new Google();
        private Apple apple = new Apple();

        @Getter
        @Setter
        public static class Google {
            private String clientId = "czyzlowie-google-client-id";
        }

        @Getter
        @Setter
        public static class Apple {
            private String clientId = "pl.czyzlowie.app";
        }
    }
}
