package pl.matkmiec.backend.weather.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.matkmiec.backend.weather.dto.ImgwHydroResponseDto;
import pl.matkmiec.backend.weather.dto.ImgwMeteoResponseDto;
import pl.matkmiec.backend.weather.dto.ImgwSynopResponseDto;
import pl.matkmiec.backend.weather.model.ImgwHydroData;
import pl.matkmiec.backend.weather.model.ImgwHydroStation;
import pl.matkmiec.backend.weather.model.ImgwMeteoData;
import pl.matkmiec.backend.weather.model.ImgwMeteoStation;
import pl.matkmiec.backend.weather.model.ImgwSynopData;
import pl.matkmiec.backend.weather.model.ImgwSynopStation;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ImgwDataMapperTest {

    private ImgwDataMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ImgwDataMapper();
    }

    @Test
    void shouldMapSynopDtoToEntity() {
        ImgwSynopResponseDto dto = new ImgwSynopResponseDto(
                "12295",
                "Białystok",
                "2026-10-07",
                "13",
                18.2,
                1.0,
                290,
                60.6,
                0.0,
                1017.8
        );
        ImgwSynopStation station = ImgwSynopStation.builder().id("12295").name("Białystok").isActive(true).build();

        ImgwSynopData entity = mapper.toSynopDataEntity(dto, station);

        assertThat(entity).isNotNull();
        assertThat(entity.getStation().getId()).isEqualTo("12295");
        assertThat(entity.getMeasurementTime()).isEqualTo(LocalDateTime.of(2026, 10, 7, 13, 0));
        assertThat(entity.getTemperature()).isEqualTo(18.2);
        assertThat(entity.getWindSpeed()).isEqualTo(1.0);
        assertThat(entity.getWindDirection()).isEqualTo(290);
        assertThat(entity.getRelativeHumidity()).isEqualTo(60.6);
        assertThat(entity.getPrecipitation()).isEqualTo(0.0);
        assertThat(entity.getPressure()).isEqualTo(1017.8);
    }

    @Test
    void shouldMapHydroDtoToEntity() {
        ImgwHydroResponseDto dto = new ImgwHydroResponseDto(
                "151140030",
                "Przewoźniki",
                "Skroda",
                "lubuskie",
                51.5253,
                14.8217,
                1957,
                114.049,
                4.22,
                340.0,
                300.0,
                226.0,
                "2026-10-07 13:20:00",
                15.5,
                "2026-10-07 13:20:00",
                0.11,
                "2026-02-18 09:50:00",
                0,
                "2026-02-26 11:20:00",
                0,
                "2026-10-06 11:50:00"
        );
        ImgwHydroStation station = ImgwHydroStation.builder().id("151140030").name("Przewoźniki").isActive(true).build();

        ImgwHydroData entity = mapper.toHydroDataEntity(dto, station);

        assertThat(entity).isNotNull();
        assertThat(entity.getStation().getId()).isEqualTo("151140030");
        assertThat(entity.getMeasurementTime()).isEqualTo(LocalDateTime.of(2026, 10, 7, 13, 20, 0));
        assertThat(entity.getWaterLevel()).isEqualTo(226.0);
        assertThat(entity.getWaterTemperature()).isEqualTo(15.5);
        assertThat(entity.getFlow()).isEqualTo(0.11);
        assertThat(entity.getIcePhenomenon()).isEqualTo(0);
        assertThat(entity.getOvergrowthPhenomenon()).isEqualTo(0);
    }

    @Test
    void shouldMapMeteoDtoWithFallbackTimestamp() {
        ImgwMeteoResponseDto dto = new ImgwMeteoResponseDto(
                "252210290",
                "RYBIENKO",
                21.429167,
                52.577778,
                2014,
                93,
                null,
                null,
                null,
                null,
                174.9,
                "2026-10-07 13:20:00",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                0.0,
                "2026-10-07 13:20:00"
        );
        ImgwMeteoStation station = ImgwMeteoStation.builder().id("252210290").name("RYBIENKO").isActive(true).build();

        ImgwMeteoData entity = mapper.toMeteoDataEntity(dto, station);

        assertThat(entity).isNotNull();
        assertThat(entity.getStation().getId()).isEqualTo("252210290");
        assertThat(entity.getMeasurementTime()).isEqualTo(LocalDateTime.of(2026, 10, 7, 13, 20, 0));
        assertThat(entity.getWindDirection()).isEqualTo(175);
        assertThat(entity.getPrecipitation10min()).isEqualTo(0.0);
    }

    @Test
    void shouldDeserializeMeteoJsonWithDecimalsAndStrings() throws Exception {
        String json = """
                [
                  {
                    "kod_stacji": "252210290",
                    "nazwa_stacji": "RYBIENKO",
                    "lon": "21.429167",
                    "lat": "52.577778",
                    "rok_zalozenia_stacji": "2014",
                    "wysokosc_npm": "93",
                    "temperatura_gruntu": null,
                    "temperatura_gruntu_data": null,
                    "temperatura_powietrza": null,
                    "temperatura_powietrza_data": null,
                    "wiatr_kierunek": "174.9",
                    "wiatr_kierunek_data": "2026-10-08 10:00:00",
                    "wiatr_srednia_predkosc": null,
                    "wiatr_srednia_predkosc_data": null,
                    "wiatr_predkosc_maksymalna": null,
                    "wiatr_predkosc_maksymalna_data": null,
                    "wilgotnosc_wzgledna": null,
                    "wilgotnosc_wzgledna_data": null,
                    "wiatr_poryw_10min": null,
                    "wiatr_poryw_10min_data": null,
                    "opad_10min": "0",
                    "opad_10min_data": "2026-10-08 10:00:00"
                  },
                  {
                    "kod_stacji": "351230497",
                    "nazwa_stacji": "WŁODAWA",
                    "lon": "23.529444",
                    "lat": "51.553333",
                    "rok_zalozenia_stacji": "1954",
                    "wysokosc_npm": "177",
                    "temperatura_gruntu": "26.5",
                    "temperatura_gruntu_data": "2026-10-08 10:00:00",
                    "temperatura_powietrza": "19.2",
                    "temperatura_powietrza_data": "2026-10-08 09:10:00",
                    "wiatr_kierunek": "200",
                    "wiatr_kierunek_data": "2026-10-08 10:00:00",
                    "wiatr_srednia_predkosc": "6.4",
                    "wiatr_srednia_predkosc_data": "2026-10-08 10:00:00",
                    "wiatr_predkosc_maksymalna": "9.4",
                    "wiatr_predkosc_maksymalna_data": "2026-10-08 10:00:00",
                    "wilgotnosc_wzgledna": "37",
                    "wilgotnosc_wzgledna_data": "2026-10-08 10:00:00",
                    "wiatr_poryw_10min": "11",
                    "wiatr_poryw_10min_data": "2026-09-23 18:00:00",
                    "opad_10min": "0",
                    "opad_10min_data": "2026-10-08 10:00:00"
                  }
                ]
                """;
        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        java.util.List<ImgwMeteoResponseDto> result = objectMapper.readValue(
                json,
                objectMapper.getTypeFactory().constructCollectionType(java.util.List.class, ImgwMeteoResponseDto.class)
        );

        assertThat(result).hasSize(2);
        assertThat(result.get(0).wiatrKierunek()).isEqualTo(174.9);
        assertThat(result.get(1).wiatrKierunek()).isEqualTo(200.0);
    }

    @Test
    void shouldDeserializeMeteoJsonWithEmptyStrings() throws Exception {
        String json = """
                [
                  {
                    "kod_stacji": "252210290",
                    "nazwa_stacji": "RYBIENKO",
                    "lon": "21.429167",
                    "lat": "52.577778",
                    "rok_zalozenia_stacji": "",
                    "wysokosc_npm": "",
                    "temperatura_gruntu": "",
                    "temperatura_gruntu_data": "",
                    "temperatura_powietrza": "",
                    "temperatura_powietrza_data": "",
                    "wiatr_kierunek": "",
                    "wiatr_kierunek_data": "",
                    "wiatr_srednia_predkosc": "",
                    "wiatr_srednia_predkosc_data": "",
                    "wiatr_predkosc_maksymalna": "",
                    "wiatr_predkosc_maksymalna_data": "",
                    "wilgotnosc_wzgledna": "",
                    "wilgotnosc_wzgledna_data": "",
                    "wiatr_poryw_10min": "",
                    "wiatr_poryw_10min_data": "",
                    "opad_10min": "",
                    "opad_10min_data": ""
                  }
                ]
                """;
        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
        objectMapper.coercionConfigFor(com.fasterxml.jackson.databind.type.LogicalType.Integer)
                .setCoercion(com.fasterxml.jackson.databind.cfg.CoercionInputShape.EmptyString, com.fasterxml.jackson.databind.cfg.CoercionAction.AsNull);
        objectMapper.coercionConfigFor(com.fasterxml.jackson.databind.type.LogicalType.Float)
                .setCoercion(com.fasterxml.jackson.databind.cfg.CoercionInputShape.EmptyString, com.fasterxml.jackson.databind.cfg.CoercionAction.AsNull);

        java.util.List<ImgwMeteoResponseDto> result = objectMapper.readValue(
                json,
                objectMapper.getTypeFactory().constructCollectionType(java.util.List.class, ImgwMeteoResponseDto.class)
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).rokZalozeniaStacji()).isNull();
        assertThat(result.get(0).temperaturaGruntu()).isNull();
    }
}
