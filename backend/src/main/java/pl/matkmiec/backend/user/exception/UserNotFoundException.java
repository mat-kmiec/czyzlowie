package pl.matkmiec.backend.user.exception;

import pl.matkmiec.backend.auth.exception.AuthException;

import java.util.UUID;

public class UserNotFoundException extends AuthException {
    public UserNotFoundException(String message) {
        super(message);
    }

    public UserNotFoundException(UUID userId) {
        super("Nie znaleziono użytkownika o identyfikatorze: " + userId);
    }
}
