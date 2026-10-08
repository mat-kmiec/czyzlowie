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

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                CACHE_SYNOP_STATIONS,
                CACHE_METEO_STATIONS,
                CACHE_HYDRO_STATIONS,
                CACHE_CURRENT_WEATHER,
                CACHE_NEAREST_STATION,
                CACHE_FORECAST
        );
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(500)
                .recordStats());
        return cacheManager;
    }
}
