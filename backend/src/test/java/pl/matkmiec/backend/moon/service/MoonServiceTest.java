package pl.matkmiec.backend.moon.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.matkmiec.backend.airquality.repository.GiosStationRepository;
import pl.matkmiec.backend.moon.dto.MoonDailyDto;
import pl.matkmiec.backend.moon.dto.MoonGenerationResultDto;
import pl.matkmiec.backend.moon.dto.MoonMonthlyDto;
import pl.matkmiec.backend.moon.dto.MoonYearlyAdminDto;
import pl.matkmiec.backend.moon.model.MoonGeneralDay;
import pl.matkmiec.backend.moon.model.MoonStationDay;
import pl.matkmiec.backend.moon.repository.MoonGeneralDayRepository;
import pl.matkmiec.backend.moon.repository.MoonStationDayRepository;
import pl.matkmiec.backend.weather.model.ImgwMeteoStation;
import pl.matkmiec.backend.weather.model.ImgwSynopStation;
import pl.matkmiec.backend.weather.repository.ImgwMeteoStationRepository;
import pl.matkmiec.backend.weather.repository.ImgwSynopStationRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MoonServiceTest {

    @Mock
    private ImgwSynopStationRepository synopStationRepo;

    @Mock
    private ImgwMeteoStationRepository meteoStationRepo;

    @Mock
    private GiosStationRepository giosStationRepo;

    @Mock
    private MoonGeneralDayRepository moonGeneralDayRepo;

    @Mock
    private MoonStationDayRepository moonStationDayRepo;

    @InjectMocks
    private MoonService moonService;

    private ImgwSynopStation mockSynopStation;
    private ImgwMeteoStation mockMeteoStation;

    @BeforeEach
    void setUp() {
        mockSynopStation = ImgwSynopStation.builder()
                .id("352200375")
                .name("WARSZAWA-OKECIE")
                .lat(52.16)
                .lon(20.96)
                .isActive(true)
                .build();

        mockMeteoStation = ImgwMeteoStation.builder()
                .id("M_WARSZAWA")
                .name("WARSZAWA-BIELANY")
                .lat(52.28)
                .lon(20.95)
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Should return daily moon data for given coordinates (fallback to calculator)")
    void shouldReturnDailyMoonDataForCoords() {
        LocalDate date = LocalDate.of(2026, 10, 13);
        when(synopStationRepo.findAllByIsActiveTrue()).thenReturn(List.of(mockSynopStation));

        MoonDailyDto result = moonService.getDailyData(52.23, 21.01, null, date);

        assertThat(result).isNotNull();
        assertThat(result.date()).isEqualTo(date);
        assertThat(result.location().latitude()).isEqualTo(52.23);
        assertThat(result.location().longitude()).isEqualTo(21.01);
        assertThat(result.generalMoon()).isNotNull();
        assertThat(result.sunTimes()).isNotNull();
        assertThat(result.moonTimes()).isNotNull();
        assertThat(result.solunar()).isNotNull();
    }

    @Test
    @DisplayName("Should return daily moon data when station ID is provided")
    void shouldReturnDailyMoonDataForStationId() {
        LocalDate date = LocalDate.of(2026, 10, 13);
        when(synopStationRepo.findById("352200375")).thenReturn(Optional.of(mockSynopStation));

        MoonDailyDto result = moonService.getDailyData(null, null, "352200375", date);

        assertThat(result).isNotNull();
        assertThat(result.location().stationId()).isEqualTo("352200375");
        assertThat(result.location().latitude()).isEqualTo(52.16);
        assertThat(result.location().longitude()).isEqualTo(20.96);
    }

    @Test
    @DisplayName("Should generate yearly moon data and save to DB in batches")
    void shouldGenerateYearlyDataAndPersist() {
        when(synopStationRepo.findAllByIsActiveTrue()).thenReturn(List.of(mockSynopStation));

        MoonGenerationResultDto result = moonService.generateYearlyData(2027);

        assertThat(result).isNotNull();
        assertThat(result.year()).isEqualTo(2027);
        assertThat(result.generalDaysProcessed()).isEqualTo(365);
        assertThat(result.stationsProcessed()).isEqualTo(1);
        assertThat(result.stationDaysProcessed()).isEqualTo(365);
        assertThat(result.status()).isEqualTo("SUCCESS");

        verify(moonGeneralDayRepo).saveAll(any());
        verify(moonStationDayRepo, atLeastOnce()).saveAll(any());
    }

    @Test
    @DisplayName("Should generate yearly moon data in parallel for multiple stations")
    void shouldGenerateYearlyDataInParallelForMultipleStations() {
        ImgwSynopStation station2 = ImgwSynopStation.builder()
                .id("350190566")
                .name("KRAKOW-BALICE")
                .lat(50.08)
                .lon(19.80)
                .isActive(true)
                .build();

        ImgwSynopStation station3 = ImgwSynopStation.builder()
                .id("354180155")
                .name("GDANSK-REBIECHOWO")
                .lat(54.38)
                .lon(18.47)
                .isActive(true)
                .build();

        when(synopStationRepo.findAllByIsActiveTrue()).thenReturn(List.of(mockSynopStation, station2, station3));

        MoonGenerationResultDto result = moonService.generateYearlyData(2027);

        assertThat(result).isNotNull();
        assertThat(result.year()).isEqualTo(2027);
        assertThat(result.generalDaysProcessed()).isEqualTo(365);
        assertThat(result.stationsProcessed()).isEqualTo(3);
        assertThat(result.stationDaysProcessed()).isEqualTo(365 * 3);
        assertThat(result.status()).isEqualTo("SUCCESS");

        verify(moonGeneralDayRepo).saveAll(any());
        verify(moonStationDayRepo, atLeastOnce()).saveAll(any());
    }

    @Test
    @DisplayName("Should return monthly calendar data")
    void shouldReturnMonthlyData() {
        when(synopStationRepo.findAllByIsActiveTrue()).thenReturn(List.of(mockSynopStation));

        MoonMonthlyDto result = moonService.getMonthlyData(52.23, 21.01, null, 2026, 10);

        assertThat(result).isNotNull();
        assertThat(result.year()).isEqualTo(2026);
        assertThat(result.month()).isEqualTo(10);
        assertThat(result.days()).hasSize(31);
        assertThat(result.bestFishingDaysCount()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("Should return yearly admin calculations")
    void shouldReturnYearlyAdminData() {
        when(synopStationRepo.findAllByIsActiveTrue()).thenReturn(List.of(mockSynopStation));

        MoonYearlyAdminDto result = moonService.getYearlyAdminData(2027, 52.23, 21.01, null);

        assertThat(result).isNotNull();
        assertThat(result.year()).isEqualTo(2027);
        assertThat(result.totalDays()).isEqualTo(365);
        assertThat(result.days()).hasSize(365);
        assertThat(result.summaryStats()).isNotEmpty();
        assertThat(result.summaryStats()).containsKey("excellentFishingDays");
        assertThat(result.summaryStats()).containsKey("topFishingDays");
    }
}
