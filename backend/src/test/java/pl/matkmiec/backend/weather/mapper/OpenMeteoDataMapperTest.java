package pl.matkmiec.backend.weather.mapper;

import org.junit.jupiter.api.Test;
import pl.matkmiec.backend.weather.dto.OpenMeteoResponseDto;
import pl.matkmiec.backend.weather.model.ImgwSynopStation;
import pl.matkmiec.backend.weather.model.OpenMeteoForecast;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OpenMeteoDataMapperTest {

    private final OpenMeteoDataMapper mapper = new OpenMeteoDataMapper();

    @Test
    void shouldMapOpenMeteoResponseToEntities() {
        OpenMeteoResponseDto.HourlyUnitsDto units = new OpenMeteoResponseDto.HourlyUnitsDto(
                "iso8601", "°C", "%", "mm", "hPa", "hPa", "m/s", "°"
        );
        OpenMeteoResponseDto.HourlyDataDto hourly = new OpenMeteoResponseDto.HourlyDataDto(
                List.of("2026-10-07T12:00", "2026-10-07T13:00"),
                List.of(15.2, 16.5),
                List.of(70.0, 65.0),
                List.of(0.0, 0.5),
                List.of(1005.0, 1004.0),
                List.of(1015.0, 1014.0),
                List.of(3.5, 4.0),
                List.of(180, 200)
        );
        OpenMeteoResponseDto dto = new OpenMeteoResponseDto(
                52.23, 21.01, 0.5, 7200, "Europe/Warsaw", "CEST", 105.0, units, hourly
        );

        ImgwSynopStation station = ImgwSynopStation.builder()
                .id("12375")
                .name("Warszawa")
                .lat(52.23)
                .lon(21.01)
                .isActive(true)
                .build();

        LocalDateTime generatedAt = LocalDateTime.of(2026, 10, 7, 10, 0);

        List<OpenMeteoForecast> forecasts = mapper.toForecastEntities(dto, station, generatedAt);

        assertThat(forecasts).hasSize(2);

        OpenMeteoForecast first = forecasts.get(0);
        assertThat(first.getSynopStation()).isEqualTo(station);
        assertThat(first.getForecastTime()).isEqualTo(LocalDateTime.of(2026, 10, 7, 12, 0));
        assertThat(first.getGeneratedAt()).isEqualTo(generatedAt);
        assertThat(first.getTemperature()).isEqualTo(15.2);
        assertThat(first.getRelativeHumidity()).isEqualTo(70.0);
        assertThat(first.getPrecipitation()).isEqualTo(0.0);
        assertThat(first.getPressure()).isEqualTo(1015.0); // should pick pressureMsl
        assertThat(first.getWindSpeed()).isEqualTo(3.5);
        assertThat(first.getWindDirection()).isEqualTo(180);

        OpenMeteoForecast second = forecasts.get(1);
        assertThat(second.getForecastTime()).isEqualTo(LocalDateTime.of(2026, 10, 7, 13, 0));
        assertThat(second.getTemperature()).isEqualTo(16.5);
        assertThat(second.getPrecipitation()).isEqualTo(0.5);
        assertThat(second.getPressure()).isEqualTo(1014.0);
    }

    @Test
    void shouldHandleNullOrEmptyInputGracefully() {
        ImgwSynopStation station = ImgwSynopStation.builder().id("12375").name("Warszawa").build();

        assertThat(mapper.toForecastEntities(null, station, LocalDateTime.now())).isEmpty();
        assertThat(mapper.toForecastEntities(new OpenMeteoResponseDto(null, null, null, null, null, null, null, null, null), station, LocalDateTime.now())).isEmpty();
    }
}
