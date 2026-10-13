package pl.matkmiec.backend.auth.exception;

public class InvalidPinException extends AuthException {
    public InvalidPinException(String message) {
        super(message);
    }
}
