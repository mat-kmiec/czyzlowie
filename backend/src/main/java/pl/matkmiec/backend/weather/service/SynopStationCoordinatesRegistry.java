package pl.matkmiec.backend.weather.service;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

@Component
public class SynopStationCoordinatesRegistry {

    public record Coordinates(double lat, double lon) {}

    private static final Map<String, Coordinates> BY_ID = new HashMap<>();
    private static final Map<String, Coordinates> BY_NORMALIZED_NAME = new HashMap<>();

    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]");

    static {
        register("12100", "Kołobrzeg", 54.1833, 16.1833);
        register("12105", "Koszalin", 54.2000, 16.1833);
        register("12115", "Ustka", 54.5833, 16.8500);
        register("12120", "Łeba", 54.7500, 17.5333);
        register("12125", "Hel", 54.6000, 18.8000);
        register("12135", "Gdańsk", 54.3833, 18.4667);
        register("12150", "Chojnice", 53.7000, 17.5500);
        register("12155", "Elbląg", 54.1667, 19.4167);
        register("12160", "Olsztyn", 53.7833, 20.4833);
        register("12185", "Kętrzyn", 54.0833, 21.3833);
        register("12195", "Suwałki", 54.1000, 22.9333);
        register("12200", "Świnoujście", 53.9167, 14.2333);
        register("12205", "Szczecin", 53.4333, 14.5500);
        register("12215", "Goleniów", 53.5833, 14.8500);
        register("12230", "Resko", 53.7667, 15.4000);
        register("12235", "Szczecinek", 53.7167, 16.7000);
        register("12250", "Toruń", 53.0333, 18.6000);
        register("12270", "Mikołajki", 53.8000, 21.5833);
        register("12272", "Mława", 53.1167, 20.3833);
        register("12280", "Ostrołęka", 53.0833, 21.5667);
        register("12285", "Szepietowo", 52.8667, 22.5333);
        register("12295", "Białystok", 53.1333, 23.1667);
        register("12300", "Gorzów Wielkopolski", 52.7333, 15.2333);
        register("12310", "Słubice", 52.3500, 14.5667);
        register("12330", "Poznań", 52.4167, 16.9167);
        register("12348", "Koło", 52.2000, 18.6333);
        register("12360", "Płock", 52.5500, 19.7000);
        register("12375", "Warszawa", 52.2333, 21.0167);
        register("12385", "Siedlce", 52.1667, 22.2667);
        register("12399", "Terespol", 52.0667, 23.6167);
        register("12400", "Zielona Góra", 51.9333, 15.5000);
        register("12415", "Legnica", 51.2000, 16.1667);
        register("12418", "Leszno", 51.8333, 16.5833);
        register("12424", "Wrocław", 51.1000, 17.0333);
        register("12425", "Wrocław-Strachowice", 51.1000, 16.8833);
        register("12435", "Kalisz", 51.7500, 18.0833);
        register("12455", "Wieluń", 51.2167, 18.5667);
        register("12465", "Łódź", 51.7500, 19.4500);
        register("12469", "Sulejów", 51.3500, 19.8667);
        register("12495", "Radom", 51.4000, 21.1500);
        register("12497", "Włodawa", 51.5500, 23.5333);
        register("12498", "Kozienice", 51.5833, 21.5500);
        register("12500", "Jelenia Góra", 50.9000, 15.7333);
        register("12510", "Śnieżka", 50.7333, 15.7333);
        register("12520", "Kłodzko", 50.4333, 16.6500);
        register("12530", "Opole", 50.6667, 17.9167);
        register("12540", "Racibórz", 50.0833, 18.2167);
        register("12550", "Częstochowa", 50.8167, 19.1167);
        register("12560", "Katowice", 50.2500, 19.0167);
        register("12566", "Kraków", 50.0833, 19.8000);
        register("12566", "Kraków-Balice", 50.0833, 19.8000);
        register("12570", "Kielce", 50.8667, 20.6167);
        register("12575", "Tarnów", 50.0167, 20.9833);
        register("12580", "Rzeszów", 50.0333, 22.0000);
        register("12580", "Rzeszów-Jasionka", 50.0333, 22.0000);
        register("12585", "Sandomierz", 50.6833, 21.7500);
        register("12595", "Zamość", 50.7167, 23.2500);
        register("12600", "Bielsko-Biała", 49.8167, 19.0333);
        register("12600", "Bielsko Biała", 49.8167, 19.0333);
        register("12625", "Zakopane", 49.3000, 19.9667);
        register("12650", "Kasprowy Wierch", 49.2333, 19.9833);
        register("12660", "Nowy Sącz", 49.6167, 20.7000);
        register("12670", "Krościenko", 49.4333, 20.4333);
        register("12680", "Lesko", 49.4667, 22.3333);
        register("12690", "Przemyśl", 49.7833, 22.7667);
        register("12695", "Sanok", 49.5500, 22.2000);
    }

    private static void register(String id, String name, double lat, double lon) {
        Coordinates coords = new Coordinates(lat, lon);
        if (id != null && !id.isBlank()) {
            BY_ID.put(id.trim(), coords);
        }
        if (name != null && !name.isBlank()) {
            BY_NORMALIZED_NAME.put(normalize(name), coords);
        }
    }

    public Optional<Coordinates> findCoordinates(String stationId, String stationName) {
        if (stationId != null && BY_ID.containsKey(stationId.trim())) {
            return Optional.of(BY_ID.get(stationId.trim()));
        }
        if (stationName != null && !stationName.isBlank()) {
            String norm = normalize(stationName);
            if (BY_NORMALIZED_NAME.containsKey(norm)) {
                return Optional.of(BY_NORMALIZED_NAME.get(norm));
            }
        }
        return Optional.empty();
    }

    public static String normalize(String input) {
        if (input == null) return "";
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{M}", "");
        normalized = normalized.toLowerCase(Locale.ROOT);
        return NON_ALPHANUMERIC.matcher(normalized).replaceAll("");
    }
}
