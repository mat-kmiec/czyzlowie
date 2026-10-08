package pl.matkmiec.backend.weather.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class GeoUtilsTest {

    @Test
    void shouldCalculateAccurateDistanceBetweenWarsawAndKrakow() {
        // Warsaw: 52.2297, 21.0122
        // Krakow: 50.0647, 19.9450
        // Expected distance: ~252 km
        double distance = GeoUtils.calculateDistanceKm(52.2297, 21.0122, 50.0647, 19.9450);

        assertThat(distance).isCloseTo(252.0, within(2.0));
    }

    @Test
    void shouldReturnZeroForIdenticalCoordinates() {
        double distance = GeoUtils.calculateDistanceKm(52.2297, 21.0122, 52.2297, 21.0122);
        assertThat(distance).isEqualTo(0.0);
    }
}
