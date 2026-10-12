package pl.matkmiec.backend.airquality.exception;

public class AirQualityApiException extends RuntimeException {
    public AirQualityApiException(String message) {
        super(message);
    }

    public AirQualityApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
