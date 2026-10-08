package pl.matkmiec.backend.weather.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pl.matkmiec.backend.weather.model.*;
import pl.matkmiec.backend.weather.repository.*;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class WeatherCleanupServiceIntegrationTest {

    @Autowired
    private WeatherCleanupService weatherCleanupService;

    @Autowired
    private ImgwSynopStationRepository synopStationRepo;

    @Autowired
    private ImgwMeteoStationRepository meteoStationRepo;

    @Autowired
    private ImgwHydroStationRepository hydroStationRepo;

    @Autowired
    private ImgwSynopDataRepository synopDataRepo;

    @Autowired
    private ImgwMeteoDataRepository meteoDataRepo;

    @Autowired
    private ImgwHydroDataRepository hydroDataRepo;

    @Autowired
    private OpenMeteoForecastRepository openMeteoForecastRepo;

    @Autowired
    private WeatherSyncLogRepository syncLogRepo;

    private ImgwSynopStation synopStation;
    private ImgwMeteoStation meteoStation;
    private ImgwHydroStation hydroStation;

    @BeforeEach
    void setUp() {
        synopStation = synopStationRepo.save(ImgwSynopStation.builder()
                .id("CLEANUP_SYNOP_1")
                .name("Stacja Synop Test")
                .isActive(true)
                .build());

        meteoStation = meteoStationRepo.save(ImgwMeteoStation.builder()
                .id("CLEANUP_METEO_1")
                .name("Stacja Meteo Test")
                .isActive(true)
                .build());

        hydroStation = hydroStationRepo.save(ImgwHydroStation.builder()
                .id("CLEANUP_HYDRO_1")
                .name("Stacja Hydro Test")
                .river("Wisła")
                .isActive(true)
                .build());
    }

    @Test
    void shouldDeleteRecordsOlderThanRetentionPeriodAndKeepRecentRecords() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
        LocalDateTime oldDate = now.minusDays(10);
        LocalDateTime recentDate = now.minusDays(2);

        // Save old and recent Synop data
        synopDataRepo.save(ImgwSynopData.builder()
                .station(synopStation)
                .measurementTime(oldDate)
                .temperature(15.0)
                .build());
        synopDataRepo.save(ImgwSynopData.builder()
                .station(synopStation)
                .measurementTime(recentDate)
                .temperature(20.0)
                .build());

        // Save old and recent Meteo data
        meteoDataRepo.save(ImgwMeteoData.builder()
                .station(meteoStation)
                .measurementTime(oldDate)
                .airTemperature(12.0)
                .build());
        meteoDataRepo.save(ImgwMeteoData.builder()
                .station(meteoStation)
                .measurementTime(recentDate)
                .airTemperature(18.0)
                .build());

        // Save old and recent Hydro data
        hydroDataRepo.save(ImgwHydroData.builder()
                .station(hydroStation)
                .measurementTime(oldDate)
                .waterLevel(150.0)
                .build());
        hydroDataRepo.save(ImgwHydroData.builder()
                .station(hydroStation)
                .measurementTime(recentDate)
                .waterLevel(160.0)
                .build());

        // Save old and recent OpenMeteo forecast
        openMeteoForecastRepo.save(OpenMeteoForecast.builder()
                .synopStation(synopStation)
                .forecastTime(oldDate)
                .generatedAt(oldDate.minusHours(1))
                .temperature(10.0)
                .build());
        openMeteoForecastRepo.save(OpenMeteoForecast.builder()
                .synopStation(synopStation)
                .forecastTime(recentDate)
                .generatedAt(recentDate.minusHours(1))
                .temperature(14.0)
                .build());

        // Save old sync log
        syncLogRepo.save(WeatherSyncLog.builder()
                .providerName("TEST_PROVIDER")
                .status("SUCCESS")
                .startedAt(oldDate)
                .finishedAt(oldDate.plusMinutes(1))
                .recordsProcessed(5)
                .build());

        // Execute cleanup for records older than 5 days
        Map<String, Object> result = weatherCleanupService.cleanupDataOlderThan(5);

        assertThat(result.get("status")).isEqualTo("SUCCESS");
        assertThat((int) result.get("synopDeleted")).isGreaterThanOrEqualTo(1);
        assertThat((int) result.get("meteoDeleted")).isGreaterThanOrEqualTo(1);
        assertThat((int) result.get("hydroDeleted")).isGreaterThanOrEqualTo(1);
        assertThat((int) result.get("openMeteoDeleted")).isGreaterThanOrEqualTo(1);
        assertThat((int) result.get("syncLogsDeleted")).isGreaterThanOrEqualTo(1);

        // Verify that recent records still exist
        assertThat(synopDataRepo.findAllByMeasurementTimeAfter(now.minusDays(3))).hasSize(1);
        assertThat(meteoDataRepo.findAllByMeasurementTimeAfter(now.minusDays(3))).hasSize(1);
        assertThat(hydroDataRepo.findAllByMeasurementTimeAfter(now.minusDays(3))).hasSize(1);
        assertThat(openMeteoForecastRepo.findBySynopStation_IdAndForecastTime(synopStation.getId(), recentDate)).isPresent();
        assertThat(openMeteoForecastRepo.findBySynopStation_IdAndForecastTime(synopStation.getId(), oldDate)).isEmpty();
    }
}
