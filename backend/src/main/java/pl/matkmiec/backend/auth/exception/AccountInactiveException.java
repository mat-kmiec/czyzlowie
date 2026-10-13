package pl.matkmiec.backend.auth.exception;

public class AccountInactiveException extends AuthException {
    public AccountInactiveException(String message) {
        super(message);
    }
}
