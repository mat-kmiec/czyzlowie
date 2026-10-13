package pl.matkmiec.backend.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailNotificationService {

    private final JavaMailSender mailSender;

    @Autowired
    public EmailNotificationService(@Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationPin(String toEmail, String pinCode, int expirationMinutes) {
        String subject = "Kod weryfikacyjny CzyZłowie: " + pinCode;
        String text = String.format(
                "Witaj w CzyZłowie!\n\n" +
                "Twój 4-cyfrowy kod weryfikacyjny: %s\n\n" +
                "Wpisz ten kod w aplikacji mobilnej lub na stronie, aby potwierdzić swój adres e-mail.\n" +
                "Kod jest ważny przez %d minut.\n\n" +
                "Życzymy udanych połowów,\nZespół CzyZłowie",
                pinCode,
                expirationMinutes
        );

        sendEmailSafely(toEmail, subject, text, "Weryfikacja e-mail PIN: " + pinCode);
    }

    public void sendPasswordResetPin(String toEmail, String pinCode, int expirationMinutes) {
        String subject = "Reset hasła CzyZłowie - Kod PIN: " + pinCode;
        String text = String.format(
                "Otrzymaliśmy prośbę o reset hasła do Twojego konta w serwisie CzyZłowie.\n\n" +
                "Twój 4-cyfrowy kod PIN do resetu hasła: %s\n\n" +
                "Wpisz ten kod w aplikacji, aby ustawić nowe hasło.\n" +
                "Kod wygasa za %d minut.\n\n" +
                "Jeśli to nie Ty prosiłeś o reset hasła, zignoruj tę wiadomość.\n\n" +
                "Pozdrawiamy,\nZespół CzyZłowie",
                pinCode,
                expirationMinutes
        );

        sendEmailSafely(toEmail, subject, text, "Reset hasła PIN: " + pinCode);
    }

    private void sendEmailSafely(String toEmail, String subject, String content, String debugPinInfo) {
        log.info("[EMAIL SIMULATION / DISPATCH] Do: {}, Temat: '{}', Treść / PIN: [{}]", toEmail, subject, debugPinInfo);

        if (mailSender != null) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(toEmail);
                message.setSubject(subject);
                message.setText(content);
                message.setFrom("no-reply@czyzlowie.pl");
                mailSender.send(message);
                log.info("Pomyślnie wysłano e-mail przez SMTP do: {}", toEmail);
            } catch (Exception e) {
                log.warn("Nie udało się wysłać e-maila przez SMTP do: {} ({}), PIN pozostaje aktywny w logu: {}",
                        toEmail, e.getMessage(), debugPinInfo);
            }
        }
    }
}
