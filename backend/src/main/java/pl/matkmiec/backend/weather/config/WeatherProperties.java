package pl.matkmiec.backend.weather.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "weather")
public record WeatherProperties(
        @DefaultValue Api api,
        @DefaultValue Sync sync,
        @DefaultValue Cleanup cleanup
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
            @DefaultValue("0 0 */6 * * *") String openMeteoCron,
            @DefaultValue("1") int openMeteoPastDays,
            @DefaultValue("6") int openMeteoForecastDays,
            @DefaultValue("50") int batchSize,
            @DefaultValue("48") int deduplicationWindowHours
    ) {}

    public record Cleanup(
            @DefaultValue("true") boolean enabled,
            @DefaultValue("0 30 1 * * *") String cron,
            @DefaultValue("Europe/Warsaw") String zone,
            @DefaultValue("5") int retentionDays
    ) {}

    public String imgwBaseUrl() {
        return api != null && api.imgwBaseUrl() != null ? api.imgwBaseUrl() : "https://danepubliczne.imgw.pl/api";
    }

    public String openMeteoBaseUrl() {
        return api != null && api.openMeteoBaseUrl() != null ? api.openMeteoBaseUrl() : "https://api.open-meteo.com/v1";
    }
}
