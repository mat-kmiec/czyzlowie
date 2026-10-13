package pl.matkmiec.backend.moon.calculator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.matkmiec.backend.moon.dto.*;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class SolunarCalculatorTest {

    private static final double WARSAW_LAT = 52.2297;
    private static final double WARSAW_LON = 21.0122;
    private static final ZoneId WARSAW_ZONE = ZoneId.of("Europe/Warsaw");

    @Test
    @DisplayName("Should generate major and minor periods with valid time intervals")
    void shouldGenerateMajorAndMinorPeriods() {
        LocalDate date = LocalDate.of(2026, 10, 13);
        GeneralMoonDataDto generalMoon = LunarCalculator.calculateGeneralMoonData(date);
        SunTimesDto sunTimes = SolarCalculator.calculateSunTimes(WARSAW_LAT, WARSAW_LON, date, WARSAW_ZONE);
        MoonTimesDto moonTimes = LunarCalculator.calculateMoonTimes(WARSAW_LAT, WARSAW_LON, date, WARSAW_ZONE);

        SolunarDayDto solunar = SolunarCalculator.calculateSolunarDay(generalMoon, sunTimes, moonTimes);

        assertThat(solunar).isNotNull();
        assertThat(solunar.overallRating()).isNotNull();
        assertThat(solunar.overallScore()).isBetween(0, 100);
        assertThat(solunar.stars()).isBetween(1, 5);
        assertThat(solunar.summary()).isNotEmpty();
        assertThat(solunar.majorPeriods()).isNotEmpty();
        assertThat(solunar.allPeriods()).isNotEmpty();
        assertThat(solunar.fishingTip()).isNotEmpty();

        for (SolunarPeriodDto period : solunar.majorPeriods()) {
            assertThat(period.type()).isEqualTo(SolunarPeriodType.MAJOR);
            assertThat(period.startTime()).isNotNull();
            assertThat(period.endTime()).isNotNull();
        }
    }
}
