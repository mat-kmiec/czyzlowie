package pl.matkmiec.backend.airquality.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.matkmiec.backend.airquality.dto.*;
import pl.matkmiec.backend.airquality.model.GiosAirQualityIndex;
import pl.matkmiec.backend.airquality.model.GiosMeasurement;
import pl.matkmiec.backend.airquality.model.GiosSensor;
import pl.matkmiec.backend.airquality.model.GiosStation;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class GiosDataMapperTest {

    private GiosDataMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new GiosDataMapper();
    }

    @Test
    void shouldMapStationDtoToEntity() {
        GiosCommuneDto commune = new GiosCommuneDto("Warszawa", "Warszawa", "MAZOWIECKIE");
        GiosCityDto city = new GiosCityDto(1006L, "Warszawa", commune);
        GiosStationResponseDto dto = new GiosStationResponseDto(
                114L,
                "Warszawa-Kondratowicza",
                52.2909,
                21.0345,
                city,
                "ul. Kondratowicza 8"
        );

        GiosStation entity = mapper.toStationEntity(dto);

        assertNotNull(entity);
        assertEquals(114L, entity.getId());
        assertEquals("Warszawa-Kondratowicza", entity.getStationName());
        assertEquals(52.2909, entity.getGegrLat());
        assertEquals(21.0345, entity.getGegrLon());
        assertEquals("Warszawa", entity.getCityName());
        assertEquals("MAZOWIECKIE", entity.getProvinceName());
        assertEquals("Warszawa", entity.getDistrictName());
        assertEquals("ul. Kondratowicza 8", entity.getAddressStreet());
        assertTrue(entity.getIsActive());
    }

    @Test
    void shouldMapV1FlatStationDtoToEntity() {
        GiosStationResponseDto dto = new GiosStationResponseDto(
                11L,
                "Czerniawa",
                50.912475,
                15.312190,
                null,
                "ul. Strażacka 7",
                142L,
                "Czerniawa",
                "Świeradów-Zdrój",
                "lubański",
                "DOLNOŚLĄSKIE"
        );

        GiosStation entity = mapper.toStationEntity(dto);

        assertNotNull(entity);
        assertEquals(11L, entity.getId());
        assertEquals("Czerniawa", entity.getStationName());
        assertEquals(50.912475, entity.getGegrLat());
        assertEquals(15.312190, entity.getGegrLon());
        assertEquals(142L, entity.getCityId());
        assertEquals("Czerniawa", entity.getCityName());
        assertEquals("Świeradów-Zdrój", entity.getCommuneName());
        assertEquals("lubański", entity.getDistrictName());
        assertEquals("DOLNOŚLĄSKIE", entity.getProvinceName());
        assertEquals("ul. Strażacka 7", entity.getAddressStreet());
        assertTrue(entity.getIsActive());
    }

    @Test
    void shouldMapSensorDtoToEntity() {
        GiosStation station = GiosStation.builder().id(114L).stationName("Warszawa-Kondratowicza").build();
        GiosSensorParamDto param = new GiosSensorParamDto("pył zawieszony PM10", "PM10", "PM10", 3);
        GiosSensorResponseDto dto = new GiosSensorResponseDto(650L, 114L, param);

        GiosSensor entity = mapper.toSensorEntity(dto, station);

        assertNotNull(entity);
        assertEquals(650L, entity.getId());
        assertEquals(station, entity.getStation());
        assertEquals("PM10", entity.getParamCode());
        assertEquals("PM10", entity.getParamFormula());
        assertEquals("pył zawieszony PM10", entity.getParamName());
        assertTrue(entity.getIsActive());
    }

    @Test
    void shouldMapV1FlatSensorDtoToEntity() {
        GiosStation station = GiosStation.builder().id(11L).stationName("Czerniawa").build();
        GiosSensorResponseDto dto = new GiosSensorResponseDto(
                54L,
                11L,
                null,
                "pył zawieszony PM10",
                "PM10",
                "PM10",
                3
        );

        GiosSensor entity = mapper.toSensorEntity(dto, station);

        assertNotNull(entity);
        assertEquals(54L, entity.getId());
        assertEquals(station, entity.getStation());
        assertEquals("PM10", entity.getParamCode());
        assertEquals("PM10", entity.getParamFormula());
        assertEquals("pył zawieszony PM10", entity.getParamName());
        assertEquals(3, entity.getIdParam());
        assertTrue(entity.getIsActive());
    }

    @Test
    void shouldMapMeasurementDtoToEntity() {
        GiosStation station = GiosStation.builder().id(114L).build();
        GiosSensor sensor = GiosSensor.builder().id(650L).station(station).paramCode("PM10").build();
        GiosMeasurementValueDto valueDto = new GiosMeasurementValueDto("2026-10-12 21:00:00", 24.5);

        GiosMeasurement entity = mapper.toMeasurementEntity(valueDto, sensor, "PM10");

        assertNotNull(entity);
        assertEquals(sensor, entity.getSensor());
        assertEquals(114L, entity.getStationId());
        assertEquals("PM10", entity.getParamCode());
        assertEquals(24.5, entity.getValue());
        assertEquals(LocalDateTime.of(2026, 10, 12, 21, 0), entity.getMeasurementTime());
    }

    @Test
    void shouldMapIndexDtoToEntity() {
        GiosStation station = GiosStation.builder().id(114L).build();
        GiosIndexResponseDto dto = new GiosIndexResponseDto(
                114L,
                "2026-10-12 21:15:00",
                new GiosIndexLevelDto(1, "Dobry"),
                "2026-10-12 21:00:00",
                true,
                "PM10",
                "2026-10-12 21:15:00",
                new GiosIndexLevelDto(1, "Dobry"),
                "2026-10-12 21:00:00",
                "2026-10-12 21:15:00",
                new GiosIndexLevelDto(0, "Bardzo dobry"),
                "2026-10-12 21:00:00",
                null, null, null,
                "2026-10-12 21:15:00",
                new GiosIndexLevelDto(0, "Bardzo dobry"),
                "2026-10-12 21:00:00",
                null, null, null,
                null, null, null,
                null, null, null
        );

        GiosAirQualityIndex entity = mapper.toIndexEntity(dto, station);

        assertNotNull(entity);
        assertEquals(station, entity.getStation());
        assertEquals(1, entity.getIndexLevelId());
        assertEquals("Dobry", entity.getIndexLevelName());
        assertEquals("PM10", entity.getStIndexCrParam());
        assertEquals("Dobry", entity.getPm10IndexLevel());
        assertEquals("Bardzo dobry", entity.getPm25IndexLevel());
    }

    @Test
    void shouldMapV1FlatIndexDtoToEntity() {
        GiosStation station = GiosStation.builder().id(11L).build();
        GiosIndexResponseDto dto = new GiosIndexResponseDto(
                11L,
                "2026-10-13 13:20:28",
                null,
                0,
                "Bardzo dobry",
                "2026-10-13 12:00:00",
                true,
                "OZON",
                "2026-10-13 13:20:28",
                null,
                "Bardzo dobry",
                "2026-10-13 12:00:00",
                null,
                null,
                null,
                null,
                "2026-10-13 13:20:28",
                null,
                "Bardzo dobry",
                "2026-10-13 13:00:00",
                "2026-10-13 13:20:28",
                null,
                "Bardzo dobry",
                "2026-10-13 13:00:00",
                "2026-10-13 13:20:28",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        GiosAirQualityIndex entity = mapper.toIndexEntity(dto, station);

        assertNotNull(entity);
        assertEquals(station, entity.getStation());
        assertEquals(0, entity.getIndexLevelId());
        assertEquals("Bardzo dobry", entity.getIndexLevelName());
        assertEquals("OZON", entity.getStIndexCrParam());
        assertEquals("Bardzo dobry", entity.getPm10IndexLevel());
        assertEquals("Bardzo dobry", entity.getO3IndexLevel());
        assertEquals("Bardzo dobry", entity.getNo2IndexLevel());
        assertNull(entity.getPm25IndexLevel());
    }

    @Test
    void shouldParseVariousDateTimeFormats() {
        assertEquals(LocalDateTime.of(2026, 10, 12, 21, 0), mapper.parseDateTime("2026-10-12 21:00:00"));
        assertEquals(LocalDateTime.of(2026, 10, 12, 21, 0), mapper.parseDateTime("2026-10-12T21:00:00"));
        assertNull(mapper.parseDateTime(null));
        assertNull(mapper.parseDateTime("invalid-date"));
    }
}
