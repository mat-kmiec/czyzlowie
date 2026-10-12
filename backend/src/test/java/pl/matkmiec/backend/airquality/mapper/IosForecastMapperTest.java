package pl.matkmiec.backend.airquality.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.matkmiec.backend.airquality.model.IosAirQualityForecast;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IosForecastMapperTest {

    private IosForecastMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new IosForecastMapper();
    }

    @Test
    void shouldMapForecastMapToEntities() {
        Map<String, Double> data = new LinkedHashMap<>();
        data.put("20261012", 16.88);
        data.put("20261013", 19.52);
        data.put("20261014", 17.65);

        List<IosAirQualityForecast> list = mapper.toForecastEntities("1465", "PM10", data);

        assertNotNull(list);
        assertEquals(3, list.size());

        IosAirQualityForecast first = list.get(0);
        assertEquals("1465", first.getTerytCode());
        assertEquals("PM10", first.getPollutant());
        assertEquals(LocalDate.of(2026, 10, 12), first.getForecastDate());
        assertEquals(16.88, first.getForecastValue());

        IosAirQualityForecast second = list.get(1);
        assertEquals(LocalDate.of(2026, 10, 13), second.getForecastDate());
        assertEquals(19.52, second.getForecastValue());
    }

    @Test
    void shouldParseDatesProperly() {
        assertEquals(LocalDate.of(2026, 10, 12), mapper.parseDate("20261012"));
        assertEquals(LocalDate.of(2026, 10, 12), mapper.parseDate("2026-10-12"));
        assertNull(mapper.parseDate(null));
        assertNull(mapper.parseDate("invalid"));
    }
}
