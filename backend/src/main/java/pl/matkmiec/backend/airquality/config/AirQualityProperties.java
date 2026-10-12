package pl.matkmiec.backend.airquality.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "airquality")
public record AirQualityProperties(
        @DefaultValue Api api,
        @DefaultValue Sync sync,
        @DefaultValue Cleanup cleanup
) {
    public record Api(
            @DefaultValue("https://api.gios.gov.pl/pjp-api/v1/rest") String giosBaseUrl,
            @DefaultValue("https://api.prognozy.ios.edu.pl/v1") String iosBaseUrl
    ) {}

    public record Sync(
            @DefaultValue("true") boolean enabled,
            @DefaultValue("0 0 3 * * *") String stationsCron,
            @DefaultValue("0 5 3 * * *") String sensorsCron,
            @DefaultValue("0 10 * * * *") String measurementsCron,
            @DefaultValue("0 12 * * * *") String indexCron,
            @DefaultValue("0 0 */6 * * *") String forecastCron,
            @DefaultValue("50") int batchSize,
            @DefaultValue("16") int parallelThreads,
            @DefaultValue("48") int deduplicationWindowHours
    ) {}

    public record Cleanup(
            @DefaultValue("true") boolean enabled,
            @DefaultValue("0 45 1 * * *") String cron,
            @DefaultValue("Europe/Warsaw") String zone,
            @DefaultValue("7") int retentionDays
    ) {}

    public String giosBaseUrl() {
        return api != null && api.giosBaseUrl() != null ? api.giosBaseUrl() : "https://api.gios.gov.pl/pjp-api/v1/rest";
    }

    public String iosBaseUrl() {
        return api != null && api.iosBaseUrl() != null ? api.iosBaseUrl() : "https://api.prognozy.ios.edu.pl/v1";
    }
}
