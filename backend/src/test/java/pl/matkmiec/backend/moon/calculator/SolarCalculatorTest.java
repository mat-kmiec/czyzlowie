package pl.matkmiec.backend.moon.calculator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.matkmiec.backend.moon.dto.SunTimesDto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class SolarCalculatorTest {

    private static final double WARSAW_LAT = 52.2297;
    private static final double WARSAW_LON = 21.0122;
    private static final ZoneId WARSAW_ZONE = ZoneId.of("Europe/Warsaw");

    @Test
    @DisplayName("Should calculate sunrise, sunset and solar noon for Warsaw on Summer Solstice")
    void shouldCalculateSummerSolsticeTimes() {
        LocalDate summerSolstice = LocalDate.of(2026, 6, 21);
        SunTimesDto times = SolarCalculator.calculateSunTimes(WARSAW_LAT, WARSAW_LON, summerSolstice, WARSAW_ZONE);

        assertThat(times.sunrise()).isNotNull();
        assertThat(times.sunset()).isNotNull();
        assertThat(times.solarNoon()).isNotNull();

        // In Warsaw summer, sunrise is ~04:14, sunset ~21:00, noon ~12:37
        assertThat(times.sunrise().getHour()).isEqualTo(4);
        assertThat(times.sunset().getHour()).isEqualTo(21);
        assertThat(times.solarNoon().getHour()).isEqualTo(12);

        assertThat(times.dayLengthSeconds()).isGreaterThan(16 * 3600);
        assertThat(times.morningGoldenHour()).isNotNull();
        assertThat(times.morningGoldenHour().start()).isNotNull();
        assertThat(times.eveningGoldenHour()).isNotNull();
        assertThat(times.eveningGoldenHour().end()).isNotNull();
    }

    @Test
    @DisplayName("Should calculate sunrise, sunset and day length for Warsaw on Winter Solstice")
    void shouldCalculateWinterSolsticeTimes() {
        LocalDate winterSolstice = LocalDate.of(2026, 12, 21);
        SunTimesDto times = SolarCalculator.calculateSunTimes(WARSAW_LAT, WARSAW_LON, winterSolstice, WARSAW_ZONE);

        assertThat(times.sunrise()).isNotNull();
        assertThat(times.sunset()).isNotNull();

        // In Warsaw winter, sunrise is ~07:44, sunset ~15:25
        assertThat(times.sunrise().getHour()).isEqualTo(7);
        assertThat(times.sunset().getHour()).isEqualTo(15);
        assertThat(times.dayLengthSeconds()).isLessThan(8 * 3600);
    }

    @Test
    @DisplayName("Should calculate twilights and blue hours properly")
    void shouldCalculateTwilightsAndBlueHours() {
        LocalDate date = LocalDate.of(2026, 10, 13);
        SunTimesDto times = SolarCalculator.calculateSunTimes(WARSAW_LAT, WARSAW_LON, date, WARSAW_ZONE);

        assertThat(times.civilTwilight()).isNotNull();
        assertThat(times.civilTwilight().dawn()).isBefore(times.sunrise());
        assertThat(times.civilTwilight().dusk()).isAfter(times.sunset());

        assertThat(times.nauticalTwilight()).isNotNull();
        assertThat(times.nauticalTwilight().dawn()).isBefore(times.civilTwilight().dawn());

        assertThat(times.morningBlueHour()).isNotNull();
        assertThat(times.eveningBlueHour()).isNotNull();
    }
}
