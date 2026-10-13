package pl.matkmiec.backend.auth.exception;

public class NicknameAlreadyExistsException extends AuthException {
    public NicknameAlreadyExistsException(String message) {
        super(message);
    }
}
