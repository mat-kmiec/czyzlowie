package pl.matkmiec.backend.weather.exception;

public class WeatherApiClientException extends RuntimeException {
    public WeatherApiClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
