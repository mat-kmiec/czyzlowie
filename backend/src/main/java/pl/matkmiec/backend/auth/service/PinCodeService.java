package pl.matkmiec.backend.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.matkmiec.backend.auth.config.SecurityProperties;
import pl.matkmiec.backend.auth.exception.InvalidPinException;
import pl.matkmiec.backend.auth.model.AuthPinCode;
import pl.matkmiec.backend.auth.model.PinType;
import pl.matkmiec.backend.auth.repository.AuthPinCodeRepository;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PinCodeService {

    private static final int MAX_PIN_ATTEMPTS = 5;

    private final AuthPinCodeRepository pinCodeRepository;
    private final SecurityProperties securityProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public String generateAndSavePin(String email, PinType pinType) {
        String cleanEmail = email.trim().toLowerCase();

        // Unieważnij poprzednie aktywne kody PIN dla tego maila i typu
        pinCodeRepository.invalidateExistingPins(cleanEmail, pinType);

        int pinNumber = 1000 + secureRandom.nextInt(9000);
        String pinCode = String.valueOf(pinNumber);

        int expirationMinutes = securityProperties.getPin().getExpirationMinutes();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(expirationMinutes);

        AuthPinCode authPin = AuthPinCode.builder()
                .email(cleanEmail)
                .pinCode(pinCode)
                .pinType(pinType)
                .expiresAt(expiresAt)
                .isUsed(false)
                .attemptCount(0)
                .createdAt(LocalDateTime.now())
                .build();

        pinCodeRepository.save(authPin);
        log.info("Wygenerowano kod PIN dla: {}, typ: {}, ważny do: {}", cleanEmail, pinType, expiresAt);
        return pinCode;
    }

    @Transactional
    public void verifyPin(String email, String rawPin, PinType pinType) {
        String cleanEmail = email.trim().toLowerCase();
        String cleanPin = rawPin != null ? rawPin.trim() : "";

        AuthPinCode pinCode = pinCodeRepository
                .findFirstByEmailIgnoreCaseAndPinTypeAndIsUsedFalseOrderByCreatedAtDesc(cleanEmail, pinType)
                .orElseThrow(() -> new InvalidPinException("Brak aktywnego kodu PIN lub kod został już wykorzystany. Wygeneruj nowy kod."));

        if (pinCode.isExpired()) {
            pinCode.setIsUsed(true);
            pinCodeRepository.save(pinCode);
            throw new InvalidPinException("Kod PIN wygasł. Wygeneruj nowy kod weryfikacyjny.");
        }

        if (pinCode.getAttemptCount() >= MAX_PIN_ATTEMPTS) {
            pinCode.setIsUsed(true);
            pinCodeRepository.save(pinCode);
            throw new InvalidPinException("Przekroczono maksymalną liczbę prób (" + MAX_PIN_ATTEMPTS + "). Wygeneruj nowy kod PIN.");
        }

        if (!pinCode.getPinCode().equals(cleanPin)) {
            pinCode.setAttemptCount(pinCode.getAttemptCount() + 1);
            pinCodeRepository.save(pinCode);
            int remaining = MAX_PIN_ATTEMPTS - pinCode.getAttemptCount();
            throw new InvalidPinException("Nieprawidłowy kod PIN. Pozostało prób: " + remaining);
        }

        // Prawidłowy kod - oznacz jako zużyty
        pinCode.setIsUsed(true);
        pinCodeRepository.save(pinCode);
        log.info("Pomyślnie zweryfikowano kod PIN dla: {}, typ: {}", cleanEmail, pinType);
    }
}
