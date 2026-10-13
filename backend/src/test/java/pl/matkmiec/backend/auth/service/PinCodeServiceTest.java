package pl.matkmiec.backend.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.matkmiec.backend.auth.config.SecurityProperties;
import pl.matkmiec.backend.auth.exception.InvalidPinException;
import pl.matkmiec.backend.auth.model.AuthPinCode;
import pl.matkmiec.backend.auth.model.PinType;
import pl.matkmiec.backend.auth.repository.AuthPinCodeRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PinCodeServiceTest {

    @Mock
    private AuthPinCodeRepository pinCodeRepository;

    private PinCodeService pinCodeService;

    @BeforeEach
    void setUp() {
        SecurityProperties securityProperties = new SecurityProperties();
        securityProperties.getPin().setExpirationMinutes(15);
        pinCodeService = new PinCodeService(pinCodeRepository, securityProperties);
    }

    @Test
    void shouldGenerateFourDigitPinAndInvalidateOldOnes() {
        String email = "test@example.com";
        when(pinCodeRepository.save(any(AuthPinCode.class))).thenAnswer(inv -> inv.getArgument(0));

        String pin = pinCodeService.generateAndSavePin(email, PinType.EMAIL_VERIFICATION);

        assertThat(pin).hasSize(4).matches("\\d{4}");
        verify(pinCodeRepository).invalidateExistingPins(email, PinType.EMAIL_VERIFICATION);

        ArgumentCaptor<AuthPinCode> captor = ArgumentCaptor.forClass(AuthPinCode.class);
        verify(pinCodeRepository).save(captor.capture());
        AuthPinCode saved = captor.getValue();

        assertThat(saved.getEmail()).isEqualTo(email);
        assertThat(saved.getPinCode()).isEqualTo(pin);
        assertThat(saved.getPinType()).isEqualTo(PinType.EMAIL_VERIFICATION);
        assertThat(saved.getIsUsed()).isFalse();
        assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now());
    }

    @Test
    void shouldVerifyValidPinSuccessfully() {
        String email = "test@example.com";
        AuthPinCode pinCode = AuthPinCode.builder()
                .id(UUID.randomUUID())
                .email(email)
                .pinCode("4819")
                .pinType(PinType.EMAIL_VERIFICATION)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .isUsed(false)
                .attemptCount(0)
                .build();

        when(pinCodeRepository.findFirstByEmailIgnoreCaseAndPinTypeAndIsUsedFalseOrderByCreatedAtDesc(email, PinType.EMAIL_VERIFICATION))
                .thenReturn(Optional.of(pinCode));

        pinCodeService.verifyPin(email, "4819", PinType.EMAIL_VERIFICATION);

        assertThat(pinCode.getIsUsed()).isTrue();
        verify(pinCodeRepository).save(pinCode);
    }

    @Test
    void shouldThrowWhenPinIsIncorrect() {
        String email = "test@example.com";
        AuthPinCode pinCode = AuthPinCode.builder()
                .id(UUID.randomUUID())
                .email(email)
                .pinCode("4819")
                .pinType(PinType.EMAIL_VERIFICATION)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .isUsed(false)
                .attemptCount(0)
                .build();

        when(pinCodeRepository.findFirstByEmailIgnoreCaseAndPinTypeAndIsUsedFalseOrderByCreatedAtDesc(email, PinType.EMAIL_VERIFICATION))
                .thenReturn(Optional.of(pinCode));

        assertThatThrownBy(() -> pinCodeService.verifyPin(email, "0000", PinType.EMAIL_VERIFICATION))
                .isInstanceOf(InvalidPinException.class)
                .hasMessageContaining("Nieprawidłowy kod PIN");

        assertThat(pinCode.getAttemptCount()).isEqualTo(1);
        verify(pinCodeRepository).save(pinCode);
    }

    @Test
    void shouldThrowWhenPinIsExpired() {
        String email = "test@example.com";
        AuthPinCode pinCode = AuthPinCode.builder()
                .id(UUID.randomUUID())
                .email(email)
                .pinCode("4819")
                .pinType(PinType.EMAIL_VERIFICATION)
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .isUsed(false)
                .attemptCount(0)
                .build();

        when(pinCodeRepository.findFirstByEmailIgnoreCaseAndPinTypeAndIsUsedFalseOrderByCreatedAtDesc(email, PinType.EMAIL_VERIFICATION))
                .thenReturn(Optional.of(pinCode));

        assertThatThrownBy(() -> pinCodeService.verifyPin(email, "4819", PinType.EMAIL_VERIFICATION))
                .isInstanceOf(InvalidPinException.class)
                .hasMessageContaining("Kod PIN wygasł");

        assertThat(pinCode.getIsUsed()).isTrue();
    }
}
