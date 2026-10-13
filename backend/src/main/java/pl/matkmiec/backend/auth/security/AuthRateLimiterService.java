package pl.matkmiec.backend.auth.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pl.matkmiec.backend.auth.config.SecurityProperties;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class AuthRateLimiterService {

    private final SecurityProperties securityProperties;
    private final Cache<String, AtomicInteger> requestCounts;
    private final Cache<String, AtomicInteger> failedAttempts;

    public AuthRateLimiterService(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
        this.requestCounts = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(1))
                .maximumSize(50_000)
                .build();
        this.failedAttempts = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(15))
                .maximumSize(50_000)
                .build();
    }

    public boolean isAllowed(String clientIp, String endpoint) {
        if (!securityProperties.getRateLimiting().isEnabled()) {
            return true;
        }

        String key = clientIp + ":" + endpoint;
        int limit = securityProperties.getRateLimiting().getAuthLimitPerMinute();
        
        AtomicInteger counter = requestCounts.get(key, k -> new AtomicInteger(0));
        int current = counter.incrementAndGet();

        if (current > limit) {
            log.warn("Przekroczono limit zapytań dla IP: {} na endpoint: {} (próba {}/{})", clientIp, endpoint, current, limit);
            return false;
        }

        return true;
    }

    public boolean checkBruteForceAllowed(String clientIp) {
        if (!securityProperties.getRateLimiting().isEnabled()) {
            return true;
        }

        int bruteLimit = securityProperties.getRateLimiting().getBruteForceLimitPerMinute();
        AtomicInteger failed = failedAttempts.getIfPresent(clientIp);

        if (failed != null && failed.get() >= bruteLimit) {
            log.warn("Wykryto potencjalny atak brute-force z IP: {} (nieudanych prób: {})", clientIp, failed.get());
            return false;
        }

        return true;
    }

    public void recordFailedAttempt(String clientIp) {
        if (!securityProperties.getRateLimiting().isEnabled()) {
            return;
        }

        AtomicInteger counter = failedAttempts.get(clientIp, k -> new AtomicInteger(0));
        int current = counter.incrementAndGet();
        log.debug("Zarejestrowano nieudaną próbę logowania z IP: {} (licznik: {})", clientIp, current);
    }

    public void resetFailedAttempts(String clientIp) {
        failedAttempts.invalidate(clientIp);
    }
}
