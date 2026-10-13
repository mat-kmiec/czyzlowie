package pl.matkmiec.backend.moon.calculator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.matkmiec.backend.moon.dto.GeneralMoonDataDto;
import pl.matkmiec.backend.moon.dto.MoonPhase;
import pl.matkmiec.backend.moon.dto.MoonTimesDto;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class LunarCalculatorTest {

    private static final double WARSAW_LAT = 52.2297;
    private static final double WARSAW_LON = 21.0122;
    private static final ZoneId WARSAW_ZONE = ZoneId.of("Europe/Warsaw");

    @Test
    @DisplayName("Should calculate general moon data for known date")
    void shouldCalculateGeneralMoonData() {
        LocalDate date = LocalDate.of(2026, 10, 13);
        GeneralMoonDataDto data = LunarCalculator.calculateGeneralMoonData(date);

        assertThat(data.phase()).isNotNull();
        assertThat(data.phase().polishName()).isNotEmpty();
        assertThat(data.phase().weatherIcon()).isNotEmpty();
        assertThat(data.phase().iconCode()).isNotEmpty();

        assertThat(data.illuminationPercentage()).isBetween(0.0, 100.0);
        assertThat(data.moonAgeDays()).isBetween(0.0, 29.54);
        assertThat(data.distanceKm()).isBetween(350000.0, 410000.0);
        assertThat(data.distancePhenomenon()).isNotEmpty();

        assertThat(data.nextNewMoon()).isAfter(date.minusDays(1));
        assertThat(data.nextFullMoon()).isAfter(date.minusDays(1));
    }

    @Test
    @DisplayName("Should calculate moon times including transit and underfoot")
    void shouldCalculateMoonTimes() {
        LocalDate date = LocalDate.of(2026, 10, 13);
        MoonTimesDto times = LunarCalculator.calculateMoonTimes(WARSAW_LAT, WARSAW_LON, date, WARSAW_ZONE);

        assertThat(times.moonTransit()).isNotNull();
        assertThat(times.moonUnderfoot()).isNotNull();
        assertThat(times.moonAltitudeDegrees()).isNotNull();
        assertThat(times.moonAzimuthDegrees()).isNotNull();
    }
}
