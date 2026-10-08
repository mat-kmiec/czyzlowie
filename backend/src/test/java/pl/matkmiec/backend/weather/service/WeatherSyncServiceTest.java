package pl.matkmiec.backend.weather.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import pl.matkmiec.backend.weather.client.ImgwApiClient;
import pl.matkmiec.backend.weather.dto.ImgwHydroResponseDto;
import pl.matkmiec.backend.weather.dto.ImgwMeteoResponseDto;
import pl.matkmiec.backend.weather.dto.ImgwSynopResponseDto;
import pl.matkmiec.backend.weather.model.ImgwHydroData;
import pl.matkmiec.backend.weather.model.ImgwMeteoData;
import pl.matkmiec.backend.weather.model.ImgwSynopData;
import pl.matkmiec.backend.weather.model.WeatherSyncLog;
import pl.matkmiec.backend.weather.repository.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
class WeatherSyncServiceTest {

    @MockitoBean
    private ImgwApiClient imgwApiClient;

    @Autowired
    private WeatherSyncService weatherSyncService;

    @Autowired
    private ImgwSynopDataRepository synopDataRepo;

    @Autowired
    private ImgwMeteoDataRepository meteoDataRepo;

    @Autowired
    private ImgwHydroDataRepository hydroDataRepo;

    @Autowired
    private ImgwSynopStationRepository synopStationRepo;

    @Autowired
    private WeatherSyncLogRepository syncLogRepo;

    @Test
    void shouldSyncSynopDataAndDeduplicateConsecutiveCalls() {
        ImgwSynopResponseDto dto1 = new ImgwSynopResponseDto(
                "12295", "Białystok", "2026-10-07", "13", 18.2, 1.0, 290, 60.6, 0.0, 1017.8
        );
        ImgwSynopResponseDto dto2 = new ImgwSynopResponseDto(
                "12600", "Bielsko Biała", "2026-10-07", "13", 20.3, 2.0, 80, 39.2, 0.0, 1014.5
        );

        when(imgwApiClient.fetchSynop()).thenReturn(List.of(dto1, dto2));

        int savedFirst = weatherSyncService.syncSynopData();
        assertThat(savedFirst).isEqualTo(2);

        List<ImgwSynopData> synopRecords = synopDataRepo.findAll();
        assertThat(synopRecords).hasSize(2);
        assertThat(synopStationRepo.findById("12295")).isPresent();

        int savedSecond = weatherSyncService.syncSynopData();
        assertThat(savedSecond).isEqualTo(0);
        assertThat(synopDataRepo.findAll()).hasSize(2);

        List<WeatherSyncLog> logs = syncLogRepo.findAll();
        assertThat(logs).isNotEmpty();
    }

    @Test
    void shouldSyncMeteoDataAndHandleAutoDiscovery() {
        ImgwMeteoResponseDto dto = new ImgwMeteoResponseDto(
                "351230497",
                "WŁODAWA",
                23.529444,
                51.553333,
                1954,
                177,
                22.7,
                "2026-10-07 13:20:00",
                20.5,
                "2026-10-07 13:10:00",
                131.0,
                "2026-10-07 13:20:00",
                0.9,
                "2026-10-07 13:20:00",
                2.3,
                "2026-10-07 13:20:00",
                50.4,
                "2026-10-07 13:20:00",
                11.0,
                "2026-09-23 18:00:00",
                0.0,
                "2026-10-07 13:20:00"
        );

        when(imgwApiClient.fetchMeteo()).thenReturn(List.of(dto));

        int saved = weatherSyncService.syncMeteoData();
        assertThat(saved).isEqualTo(1);

        List<ImgwMeteoData> records = meteoDataRepo.findAll();
        assertThat(records).hasSize(1);
        assertThat(records.get(0).getStation().getId()).isEqualTo("351230497");
        assertThat(records.get(0).getAirTemperature()).isEqualTo(20.5);

        int duplicateSave = weatherSyncService.syncMeteoData();
        assertThat(duplicateSave).isEqualTo(0);
        assertThat(meteoDataRepo.findAll()).hasSize(1);
    }

    @Test
    void shouldSyncHydroDataAndIgnoreDuplicates() {
        ImgwHydroResponseDto dto = new ImgwHydroResponseDto(
                "153190040",
                "Bągart",
                "Dzierzgoń",
                "pomorskie",
                53.9661,
                19.3692,
                1959,
                -5.077,
                38.61,
                800.0,
                790.0,
                693.0,
                "2026-10-07 08:50:00",
                null,
                null,
                0.68,
                "2026-10-07 08:50:00",
                3,
                "2026-01-28 10:50:00",
                101,
                "2026-08-05 11:00:00"
        );

        when(imgwApiClient.fetchHydro()).thenReturn(List.of(dto));

        int saved = weatherSyncService.syncHydroData();
        assertThat(saved).isEqualTo(1);

        List<ImgwHydroData> records = hydroDataRepo.findAll();
        assertThat(records).hasSize(1);
        assertThat(records.get(0).getWaterLevel()).isEqualTo(693.0);

        int secondRun = weatherSyncService.syncHydroData();
        assertThat(secondRun).isEqualTo(0);
        assertThat(hydroDataRepo.findAll()).hasSize(1);
    }
}
