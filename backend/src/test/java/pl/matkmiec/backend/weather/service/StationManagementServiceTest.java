package pl.matkmiec.backend.weather.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pl.matkmiec.backend.weather.dto.ImgwHydroResponseDto;
import pl.matkmiec.backend.weather.dto.ImgwMeteoResponseDto;
import pl.matkmiec.backend.weather.dto.ImgwSynopResponseDto;
import pl.matkmiec.backend.weather.model.ImgwHydroStation;
import pl.matkmiec.backend.weather.model.ImgwMeteoStation;
import pl.matkmiec.backend.weather.model.ImgwSynopStation;
import pl.matkmiec.backend.weather.repository.ImgwHydroStationRepository;
import pl.matkmiec.backend.weather.repository.ImgwMeteoStationRepository;
import pl.matkmiec.backend.weather.repository.ImgwSynopStationRepository;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class StationManagementServiceTest {

    @Autowired
    private StationManagementService stationManagementService;

    @Autowired
    private ImgwSynopStationRepository synopStationRepo;

    @Autowired
    private ImgwMeteoStationRepository meteoStationRepo;

    @Autowired
    private ImgwHydroStationRepository hydroStationRepo;

    @Test
    void shouldAutoDiscoverSynopStations() {
        ImgwSynopResponseDto dto1 = new ImgwSynopResponseDto("12295", "Białystok", "2026-10-07", "13", 18.2, 1.0, 290, 60.6, 0.0, 1017.8);
        ImgwSynopResponseDto dto2 = new ImgwSynopResponseDto("12600", "Bielsko Biała", "2026-10-07", "13", 20.3, 2.0, 80, 39.2, 0.0, 1014.5);

        Map<String, ImgwSynopStation> discovered = stationManagementService.autoDiscoverSynopStations(List.of(dto1, dto2));

        assertThat(discovered).hasSize(2);
        assertThat(synopStationRepo.findById("12295")).isPresent();
        assertThat(synopStationRepo.findById("12295").get().getIsActive()).isTrue();
        assertThat(synopStationRepo.findById("12600")).isPresent();
        assertThat(synopStationRepo.findById("12600").get().getName()).isEqualTo("Bielsko Biała");
    }

    @Test
    void shouldAutoDiscoverMeteoStations() {
        ImgwMeteoResponseDto dto = new ImgwMeteoResponseDto("252210290", "RYBIENKO", 21.429167, 52.577778, 2014, 93, null, null, null, null, null, null, null, null, null, null, null, null, null, null, 0.0, "2026-10-07 13:20:00");

        Map<String, ImgwMeteoStation> discovered = stationManagementService.autoDiscoverMeteoStations(List.of(dto));

        assertThat(discovered).containsKey("252210290");
        assertThat(meteoStationRepo.findById("252210290")).isPresent();
        ImgwMeteoStation station = meteoStationRepo.findById("252210290").get();
        assertThat(station.getName()).isEqualTo("RYBIENKO");
        assertThat(station.getLat()).isEqualTo(52.577778);
        assertThat(station.getElevationAsl()).isEqualTo(93);
        assertThat(station.getIsActive()).isTrue();
    }

    @Test
    void shouldAutoDiscoverHydroStations() {
        ImgwHydroResponseDto dto = new ImgwHydroResponseDto("151140030", "Przewoźniki", "Skroda", "lubuskie", 51.5253, 14.8217, 1957, 114.049, 4.22, 340.0, 300.0, 226.0, "2026-10-07 13:20:00", null, null, 0.11, "2026-02-18 09:50:00", 0, "2026-02-26 11:20:00", 0, "2026-10-06 11:50:00");

        Map<String, ImgwHydroStation> discovered = stationManagementService.autoDiscoverHydroStations(List.of(dto));

        assertThat(discovered).containsKey("151140030");
        assertThat(hydroStationRepo.findById("151140030")).isPresent();
        ImgwHydroStation station = hydroStationRepo.findById("151140030").get();
        assertThat(station.getName()).isEqualTo("Przewoźniki");
        assertThat(station.getRiver()).isEqualTo("Skroda");
        assertThat(station.getProvince()).isEqualTo("lubuskie");
        assertThat(station.getIsActive()).isTrue();
    }

    @Test
    void shouldToggleStationActive() {
        ImgwSynopStation station = ImgwSynopStation.builder().id("99999").name("Test Station").isActive(true).build();
        synopStationRepo.save(station);

        boolean updated = stationManagementService.toggleSynopStationActive("99999");
        assertThat(updated).isFalse();
        assertThat(synopStationRepo.findById("99999").get().getIsActive()).isFalse();

        boolean toggledBack = stationManagementService.toggleSynopStationActive("99999");
        assertThat(toggledBack).isTrue();
        assertThat(synopStationRepo.findById("99999").get().getIsActive()).isTrue();
    }
}
