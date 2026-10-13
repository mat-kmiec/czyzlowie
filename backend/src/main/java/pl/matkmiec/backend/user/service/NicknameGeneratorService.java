package pl.matkmiec.backend.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.matkmiec.backend.user.repository.UserRepository;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class NicknameGeneratorService {

    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public String generateUniqueNickname(String prefix) {
        String base = (prefix != null && !prefix.isBlank()) 
                ? sanitizePrefix(prefix) 
                : "Wedkarz";

        for (int i = 0; i < 50; i++) {
            int randomNum = 1000 + secureRandom.nextInt(9000);
            String candidate = base + "_" + randomNum;
            if (!userRepository.existsByNicknameIgnoreCase(candidate)) {
                return candidate;
            }
        }

        // Fallback with longer number if collision
        long timestampSuffix = System.currentTimeMillis() % 1000000;
        return base + "_" + timestampSuffix;
    }

    private String sanitizePrefix(String prefix) {
        String cleaned = prefix.replaceAll("[^a-zA-Z0-9_]", "");
        if (cleaned.length() > 20) {
            cleaned = cleaned.substring(0, 20);
        }
        return cleaned.isEmpty() ? "Wedkarz" : cleaned;
    }
}
