package pl.matkmiec.backend.weather.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import pl.matkmiec.backend.weather.config.CacheConfig;
import pl.matkmiec.backend.weather.dto.CurrentWeatherDto;
import pl.matkmiec.backend.weather.dto.NearestStationDto;
import pl.matkmiec.backend.weather.model.ImgwSynopData;
import pl.matkmiec.backend.weather.model.ImgwSynopStation;
import pl.matkmiec.backend.weather.model.OpenMeteoForecast;
import pl.matkmiec.backend.weather.repository.ImgwSynopDataRepository;
import pl.matkmiec.backend.weather.repository.ImgwSynopStationRepository;
import pl.matkmiec.backend.weather.repository.OpenMeteoForecastRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class WeatherQueryServiceIntegrationTest {

    @Autowired
    private WeatherQueryService weatherQueryService;

    @Autowired
    private ImgwSynopStationRepository synopStationRepo;

    @Autowired
    private ImgwSynopDataRepository synopDataRepo;

    @Autowired
    private OpenMeteoForecastRepository openMeteoForecastRepo;

    @Autowired
    private CacheManager cacheManager;

    private ImgwSynopStation warszawa;
    private ImgwSynopStation krakow;

    @BeforeEach
    void setUp() {
        // Clear caches before each test
        for (String cacheName : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        }

        warszawa = synopStationRepo.save(ImgwSynopStation.builder()
                .id("INT_12375")
                .name("Warszawa-Okęcie")
                .lat(52.16)
                .lon(20.96)
                .isActive(true)
                .build());

        krakow = synopStationRepo.save(ImgwSynopStation.builder()
                .id("INT_12566")
                .name("Kraków-Balice")
                .lat(50.07)
                .lon(19.78)
                .isActive(true)
                .build());

        synopDataRepo.save(ImgwSynopData.builder()
                .station(warszawa)
                .measurementTime(LocalDateTime.now().minusMinutes(30))
                .temperature(22.5)
                .windSpeed(4.2)
                .pressure(1013.0)
                .relativeHumidity(50.0)
                .precipitation(0.0)
                .build());

        openMeteoForecastRepo.save(OpenMeteoForecast.builder()
                .synopStation(warszawa)
                .forecastTime(LocalDateTime.now().plusHours(2))
                .generatedAt(LocalDateTime.now())
                .temperature(23.0)
                .build());
    }

    @Test
    void shouldFindNearestStationByCoordinatesAndCacheResult() {
        // Point in central Warsaw: 52.23, 21.01
        NearestStationDto nearest = weatherQueryService.findNearestStation(52.23, 21.01, "SYNOP");

        assertThat(nearest).isNotNull();
        assertThat(nearest.getId()).isEqualTo("INT_12375");
        assertThat(nearest.getDistanceKm()).isLessThan(15.0);

        // Verify cache contains entry
        Cache cache = cacheManager.getCache(CacheConfig.CACHE_NEAREST_STATION);
        assertThat(cache).isNotNull();
        assertThat(cache.get("52.23_21.01_SYNOP")).isNotNull();
    }

    @Test
    void shouldGetCurrentWeatherAndCacheIt() {
        CurrentWeatherDto current = weatherQueryService.getCurrentWeatherByStationId("INT_12375");

        assertThat(current).isNotNull();
        assertThat(current.getStationId()).isEqualTo("INT_12375");
        assertThat(current.getTemperature()).isEqualTo(22.5);
        assertThat(current.getUpcomingForecast()).hasSize(1);

        Cache cache = cacheManager.getCache(CacheConfig.CACHE_CURRENT_WEATHER);
        assertThat(cache).isNotNull();
        assertThat(cache.get("INT_12375")).isNotNull();
    }

    @Test
    void shouldGetPaginatedSynopHistory() {
        Page<ImgwSynopData> history = weatherQueryService.getSynopHistory(
                "INT_12375",
                null,
                null,
                PageRequest.of(0, 10)
        );

        assertThat(history.getContent()).hasSize(1);
        assertThat(history.getContent().get(0).getTemperature()).isEqualTo(22.5);
    }
}
