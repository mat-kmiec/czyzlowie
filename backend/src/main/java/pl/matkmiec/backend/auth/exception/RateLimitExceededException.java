package pl.matkmiec.backend.auth.exception;

public class RateLimitExceededException extends AuthException {
    public RateLimitExceededException(String message) {
        super(message);
    }
}
