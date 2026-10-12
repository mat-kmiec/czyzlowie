package pl.matkmiec.backend.airquality.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.matkmiec.backend.airquality.dto.*;
import pl.matkmiec.backend.airquality.mapper.GiosDataMapper;
import pl.matkmiec.backend.airquality.model.GiosSensor;
import pl.matkmiec.backend.airquality.model.GiosStation;
import pl.matkmiec.backend.airquality.repository.GiosSensorRepository;
import pl.matkmiec.backend.airquality.repository.GiosStationRepository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AirQualityStationManagementServiceTest {

    @Mock
    private GiosStationRepository stationRepo;

    @Mock
    private GiosSensorRepository sensorRepo;

    @Mock
    private GiosDataMapper mapper;

    @InjectMocks
    private AirQualityStationManagementService service;

    private GiosStation sampleStation;

    @BeforeEach
    void setUp() {
        sampleStation = GiosStation.builder()
                .id(114L)
                .stationName("Warszawa-Kondratowicza")
                .gegrLat(52.2909)
                .gegrLon(21.0345)
                .cityName("Warszawa")
                .isActive(true)
                .build();
    }

    @Test
    void shouldAutoDiscoverStations() {
        GiosCityDto city = new GiosCityDto(1006L, "Warszawa", new GiosCommuneDto("Warszawa", "Warszawa", "MAZOWIECKIE"));
        GiosStationResponseDto dto = new GiosStationResponseDto(114L, "Warszawa-Kondratowicza", 52.2909, 21.0345, city, "Kondratowicza 8");

        when(stationRepo.findAll()).thenReturn(Collections.emptyList());
        when(mapper.toStationEntity(dto)).thenReturn(sampleStation);

        Map<Long, GiosStation> result = service.autoDiscoverStations(List.of(dto));

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.containsKey(114L));
        verify(stationRepo, times(1)).saveAll(any());
    }

    @Test
    void shouldToggleStationActive() {
        when(stationRepo.findById(114L)).thenReturn(Optional.of(sampleStation));

        boolean newState = service.toggleStationActive(114L);

        assertFalse(newState);
        assertFalse(sampleStation.getIsActive());
        verify(stationRepo, times(1)).save(sampleStation);
    }

    @Test
    void shouldUpdateStationTeryt() {
        when(stationRepo.findById(114L)).thenReturn(Optional.of(sampleStation));
        when(stationRepo.save(any(GiosStation.class))).thenAnswer(i -> i.getArgument(0));

        GiosStation updated = service.updateStationTeryt(114L, "1465");

        assertNotNull(updated);
        assertEquals("1465", updated.getTerytCode());
        verify(stationRepo, times(1)).save(sampleStation);
    }
}
