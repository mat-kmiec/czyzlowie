package pl.matkmiec.backend.airquality.exception;

public class AirQualityDataParsingException extends RuntimeException {
    public AirQualityDataParsingException(String message) {
        super(message);
    }

    public AirQualityDataParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
