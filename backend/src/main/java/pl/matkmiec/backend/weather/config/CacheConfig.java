package pl.matkmiec.backend.weather.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CACHE_SYNOP_STATIONS = "synopStations";
    public static final String CACHE_METEO_STATIONS = "meteoStations";
    public static final String CACHE_HYDRO_STATIONS = "hydroStations";
    public static final String CACHE_CURRENT_WEATHER = "currentWeather";
    public static final String CACHE_NEAREST_STATION = "nearestStation";
    public static final String CACHE_FORECAST = "forecast";

    public static final String CACHE_AIR_QUALITY_MAP = "airQualityMap";
    public static final String CACHE_AIR_QUALITY_STATIONS = "airQualityStations";
    public static final String CACHE_AIR_QUALITY_RANKINGS = "airQualityRankings";
    public static final String CACHE_AIR_QUALITY_STATION_DETAILS = "airQualityStationDetails";
    public static final String CACHE_AIR_QUALITY_NEAREST = "airQualityNearest";
    public static final String CACHE_AIR_QUALITY_FORECAST = "airQualityForecast";
    public static final String CACHE_AIR_QUALITY_OVERVIEW = "airQualityOverview";

    public static final String CACHE_MOON_DAILY = "moonDaily";
    public static final String CACHE_MOON_MONTHLY = "moonMonthly";
    public static final String CACHE_MOON_YEARLY = "moonYearly";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                CACHE_SYNOP_STATIONS,
                CACHE_METEO_STATIONS,
                CACHE_HYDRO_STATIONS,
                CACHE_CURRENT_WEATHER,
                CACHE_NEAREST_STATION,
                CACHE_FORECAST,
                CACHE_AIR_QUALITY_MAP,
                CACHE_AIR_QUALITY_STATIONS,
                CACHE_AIR_QUALITY_RANKINGS,
                CACHE_AIR_QUALITY_STATION_DETAILS,
                CACHE_AIR_QUALITY_NEAREST,
                CACHE_AIR_QUALITY_FORECAST,
                CACHE_AIR_QUALITY_OVERVIEW,
                CACHE_MOON_DAILY,
                CACHE_MOON_MONTHLY,
                CACHE_MOON_YEARLY
        );
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(500)
                .recordStats());
        return cacheManager;
    }
}
