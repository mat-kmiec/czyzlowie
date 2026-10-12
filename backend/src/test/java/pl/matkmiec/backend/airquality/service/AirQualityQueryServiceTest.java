package pl.matkmiec.backend.airquality.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.matkmiec.backend.airquality.dto.*;
import pl.matkmiec.backend.airquality.model.*;
import pl.matkmiec.backend.airquality.repository.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AirQualityQueryServiceTest {

    @Mock
    private GiosStationRepository stationRepo;

    @Mock
    private GiosSensorRepository sensorRepo;

    @Mock
    private GiosMeasurementRepository measurementRepo;

    @Mock
    private GiosAirQualityIndexRepository indexRepo;

    @Mock
    private IosAirQualityForecastRepository forecastRepo;

    @InjectMocks
    private AirQualityQueryService service;

    private GiosStation stationWaw;
    private GiosStation stationKrk;
    private GiosAirQualityIndex indexWaw;
    private GiosMeasurement measPm10;

    @BeforeEach
    void setUp() {
        stationWaw = GiosStation.builder()
                .id(114L)
                .stationName("Warszawa-Kondratowicza")
                .cityName("Warszawa")
                .districtName("Warszawa")
                .provinceName("MAZOWIECKIE")
                .gegrLat(52.2909)
                .gegrLon(21.0345)
                .terytCode("1465")
                .isActive(true)
                .build();

        stationKrk = GiosStation.builder()
                .id(400L)
                .stationName("Kraków-Aleje")
                .cityName("Kraków")
                .districtName("Kraków")
                .provinceName("MAŁOPOLSKIE")
                .gegrLat(50.0620)
                .gegrLon(19.9409)
                .terytCode("1261")
                .isActive(true)
                .build();

        indexWaw = GiosAirQualityIndex.builder()
                .id(1L)
                .station(stationWaw)
                .indexLevelId(1)
                .indexLevelName("Dobry")
                .pm10IndexLevel("Dobry")
                .pm25IndexLevel("Bardzo dobry")
                .stIndexCrParam("PM10")
                .build();

        measPm10 = GiosMeasurement.builder()
                .id(100L)
                .stationId(114L)
                .paramCode("PM10")
                .measurementTime(LocalDateTime.now().minusMinutes(30))
                .value(25.4)
                .build();
    }

    @Test
    void shouldReturnMapPoints() {
        when(stationRepo.findAllActiveWithCoordinates()).thenReturn(List.of(stationWaw));
        when(indexRepo.findAllByStationIdIn(List.of(114L))).thenReturn(List.of(indexWaw));
        when(measurementRepo.findRecentByStationIds(eq(List.of(114L)), any(LocalDateTime.class))).thenReturn(List.of(measPm10));

        List<AirQualityMapPointDto> points = service.getMapPoints();

        assertNotNull(points);
        assertEquals(1, points.size());
        AirQualityMapPointDto p = points.get(0);
        assertEquals(114L, p.stationId());
        assertEquals("Warszawa-Kondratowicza", p.stationName());
        assertEquals("Dobry", p.overallIndexLevelName());
        assertTrue(p.pollutants().containsKey("PM10"));
        assertEquals(25.4, p.pollutants().get("PM10").value());
    }

    @Test
    void shouldReturnRankingsByPollutant() {
        when(stationRepo.findAllActiveWithCoordinates()).thenReturn(List.of(stationWaw));
        when(measurementRepo.findRecentByParamCode(eq("PM10"), any(LocalDateTime.class))).thenReturn(List.of(measPm10));
        when(indexRepo.findAllByStationIdIn(List.of(114L))).thenReturn(List.of(indexWaw));

        List<AirQualityRankingDto> rankings = service.getRankings("PM10", "BEST", 10);

        assertNotNull(rankings);
        assertEquals(1, rankings.size());
        AirQualityRankingDto r = rankings.get(0);
        assertEquals(1, r.rank());
        assertEquals("PM10", r.paramCode());
        assertEquals(25.4, r.value());
        assertEquals("Warszawa", r.cityName());
    }

    @Test
    void shouldFindNearestStation() {
        when(stationRepo.findAllActiveWithCoordinates()).thenReturn(List.of(stationWaw, stationKrk));
        when(indexRepo.findByStationId(114L)).thenReturn(Optional.of(indexWaw));
        when(measurementRepo.findRecentByStationIds(eq(List.of(114L)), any(LocalDateTime.class))).thenReturn(List.of(measPm10));

        // Coordinates close to Warszawa
        NearestAirQualityStationDto nearest = service.findNearestStation(52.23, 21.01);

        assertNotNull(nearest);
        assertEquals(114L, nearest.station().getId());
        assertEquals("Dobry", nearest.overallIndexLevelName());
        assertTrue(nearest.distanceKm() > 0.0);
        assertTrue(nearest.pollutants().containsKey("PM10"));
    }

    @Test
    void shouldReturnStationDetails() {
        when(stationRepo.findById(114L)).thenReturn(Optional.of(stationWaw));
        when(indexRepo.findByStationId(114L)).thenReturn(Optional.of(indexWaw));
        when(sensorRepo.findAllByStationId(114L)).thenReturn(List.of(
                GiosSensor.builder().id(650L).station(stationWaw).paramCode("PM10").paramName("pył PM10").build()
        ));
        when(measurementRepo.findAllByStationIdAndMeasurementTimeAfterOrderByMeasurementTimeAsc(eq(114L), any(LocalDateTime.class)))
                .thenReturn(List.of(measPm10));

        AirQualityStationDetailsDto details = service.getStationDetails(114L);

        assertNotNull(details);
        assertEquals(stationWaw, details.station());
        assertEquals(indexWaw, details.airQualityIndex());
        assertTrue(details.currentMeasurements().containsKey("PM10"));
        assertTrue(details.historical24h().containsKey("PM10"));
    }

    @Test
    void shouldReturnForecastForTeryt() {
        IosAirQualityForecast f1 = IosAirQualityForecast.builder()
                .terytCode("1465")
                .pollutant("PM10")
                .forecastDate(LocalDate.now().plusDays(1))
                .forecastValue(18.5)
                .build();

        when(forecastRepo.findAllByTerytCodeAndForecastDateGreaterThanEqualOrderByForecastDateAsc(eq("1465"), any(LocalDate.class)))
                .thenReturn(List.of(f1));

        List<AirQualityForecastDto> forecasts = service.getForecast("1465", null);

        assertNotNull(forecasts);
        assertEquals(1, forecasts.size());
        assertEquals("PM10", forecasts.get(0).pollutant());
        assertEquals(1, forecasts.get(0).items().size());
        assertEquals(18.5, forecasts.get(0).items().get(0).value());
    }
}
