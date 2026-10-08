package pl.matkmiec.backend.weather.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import pl.matkmiec.backend.weather.dto.CurrentWeatherDto;
import pl.matkmiec.backend.weather.dto.NearestStationDto;
import pl.matkmiec.backend.weather.dto.NearestStationsOverviewDto;
import pl.matkmiec.backend.weather.model.*;
import pl.matkmiec.backend.weather.repository.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeatherQueryServiceTest {

    @Mock
    private ImgwSynopStationRepository synopStationRepo;

    @Mock
    private ImgwMeteoStationRepository meteoStationRepo;

    @Mock
    private ImgwHydroStationRepository hydroStationRepo;

    @Mock
    private ImgwSynopDataRepository synopDataRepo;

    @Mock
    private ImgwMeteoDataRepository meteoDataRepo;

    @Mock
    private ImgwHydroDataRepository hydroDataRepo;

    @Mock
    private OpenMeteoForecastRepository openMeteoForecastRepo;

    @InjectMocks
    private WeatherQueryService weatherQueryService;

    private ImgwSynopStation warszawaSynop;
    private ImgwSynopStation krakowSynop;
    private ImgwMeteoStation warszawaMeteo;
    private ImgwHydroStation wislaHydro;

    @BeforeEach
    void setUp() {
        warszawaSynop = ImgwSynopStation.builder()
                .id("12375")
                .name("Warszawa")
                .lat(52.2297)
                .lon(21.0122)
                .isActive(true)
                .build();

        krakowSynop = ImgwSynopStation.builder()
                .id("12566")
                .name("Kraków")
                .lat(50.0647)
                .lon(19.9450)
                .isActive(true)
                .build();

        warszawaMeteo = ImgwMeteoStation.builder()
                .id("252200150")
                .name("Warszawa-Bielany")
                .lat(52.28)
                .lon(20.94)
                .isActive(true)
                .build();

        wislaHydro = ImgwHydroStation.builder()
                .id("152210010")
                .name("Warszawa-Nadwilanówka")
                .lat(52.17)
                .lon(21.11)
                .isActive(true)
                .build();
    }

    @Test
    void shouldFindNearestSynopStation() {
        when(synopStationRepo.findAllByIsActiveTrue()).thenReturn(List.of(warszawaSynop, krakowSynop));

        // Location close to Warsaw: 52.24, 21.00
        NearestStationDto result = weatherQueryService.findNearestStation(52.24, 21.00, "SYNOP");

        assertThat(result.getId()).isEqualTo("12375");
        assertThat(result.getName()).isEqualTo("Warszawa");
        assertThat(result.getStationType()).isEqualTo("SYNOP");
        assertThat(result.getDistanceKm()).isLessThan(5.0);
    }

    @Test
    void shouldFindNearestStationsOverview() {
        when(synopStationRepo.findAllByIsActiveTrue()).thenReturn(List.of(warszawaSynop));
        when(meteoStationRepo.findAllByIsActiveTrue()).thenReturn(List.of(warszawaMeteo));
        when(hydroStationRepo.findAllByIsActiveTrue()).thenReturn(List.of(wislaHydro));

        NearestStationsOverviewDto overview = weatherQueryService.findNearestStationsOverview(52.23, 21.01);

        assertThat(overview.getNearestSynop()).isNotNull();
        assertThat(overview.getNearestSynop().getId()).isEqualTo("12375");
        assertThat(overview.getNearestMeteo()).isNotNull();
        assertThat(overview.getNearestMeteo().getId()).isEqualTo("252200150");
        assertThat(overview.getNearestHydro()).isNotNull();
        assertThat(overview.getNearestHydro().getId()).isEqualTo("152210010");
    }

    @Test
    void shouldGetCurrentWeatherByStationId() {
        when(synopStationRepo.findById("12375")).thenReturn(Optional.of(warszawaSynop));

        ImgwSynopData synopData = ImgwSynopData.builder()
                .id(1L)
                .station(warszawaSynop)
                .measurementTime(LocalDateTime.of(2026, 10, 8, 14, 0))
                .temperature(20.5)
                .windSpeed(3.8)
                .pressure(1015.2)
                .relativeHumidity(55.0)
                .build();

        when(synopDataRepo.findFirstByStation_IdOrderByMeasurementTimeDesc("12375"))
                .thenReturn(Optional.of(synopData));

        OpenMeteoForecast forecast = OpenMeteoForecast.builder()
                .id(1L)
                .synopStation(warszawaSynop)
                .forecastTime(LocalDateTime.of(2026, 10, 8, 15, 0))
                .temperature(21.0)
                .build();

        when(openMeteoForecastRepo.findAllBySynopStation_IdAndForecastTimeAfterOrderByForecastTimeAsc(eq("12375"), any(LocalDateTime.class)))
                .thenReturn(List.of(forecast));

        CurrentWeatherDto current = weatherQueryService.getCurrentWeatherByStationId("12375");

        assertThat(current.getStationId()).isEqualTo("12375");
        assertThat(current.getStationName()).isEqualTo("Warszawa");
        assertThat(current.getTemperature()).isEqualTo(20.5);
        assertThat(current.getWindSpeed()).isEqualTo(3.8);
        assertThat(current.getUpcomingForecast()).hasSize(1);
    }

    @Test
    void shouldGetCurrentWeatherByCoordinates() {
        when(synopStationRepo.findAllByIsActiveTrue()).thenReturn(List.of(warszawaSynop, krakowSynop));
        when(synopStationRepo.findById("12375")).thenReturn(Optional.of(warszawaSynop));

        ImgwSynopData synopData = ImgwSynopData.builder()
                .id(1L)
                .station(warszawaSynop)
                .measurementTime(LocalDateTime.of(2026, 10, 8, 14, 0))
                .temperature(20.5)
                .build();

        when(synopDataRepo.findFirstByStation_IdOrderByMeasurementTimeDesc("12375"))
                .thenReturn(Optional.of(synopData));

        CurrentWeatherDto current = weatherQueryService.getCurrentWeatherByCoordinates(52.24, 21.00);

        assertThat(current.getStationId()).isEqualTo("12375");
        assertThat(current.getDistanceKm()).isNotNull();
        assertThat(current.getTemperature()).isEqualTo(20.5);
    }

    @Test
    void shouldGetPaginatedSynopHistory() {
        Pageable pageable = PageRequest.of(0, 10);
        ImgwSynopData data = ImgwSynopData.builder().id(100L).temperature(17.2).build();
        when(synopDataRepo.findAll(pageable)).thenReturn(new PageImpl<>(List.of(data)));

        Page<ImgwSynopData> page = weatherQueryService.getSynopHistory(null, null, null, pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getTemperature()).isEqualTo(17.2);
    }
}
