package pl.matkmiec.backend.weather.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "weather")
public record WeatherProperties(
        @DefaultValue Api api,
        @DefaultValue Sync sync
) {
    public record Api(
            @DefaultValue("https://danepubliczne.imgw.pl/api") String imgwBaseUrl,
            @DefaultValue("https://api.open-meteo.com/v1") String openMeteoBaseUrl
    ) {}

    public record Sync(
            @DefaultValue("true") boolean enabled,
            @DefaultValue("0 15 * * * *") String synopCron,
            @DefaultValue("0 */10 * * * *") String meteoCron,
            @DefaultValue("0 */10 * * * *") String hydroCron,
            @DefaultValue("50") int batchSize,
            @DefaultValue("48") int deduplicationWindowHours
    ) {}

    public String imgwBaseUrl() {
        return api != null && api.imgwBaseUrl() != null ? api.imgwBaseUrl() : "https://danepubliczne.imgw.pl/api";
    }

    public String openMeteoBaseUrl() {
        return api != null && api.openMeteoBaseUrl() != null ? api.openMeteoBaseUrl() : "https://api.open-meteo.com/v1";
    }
}