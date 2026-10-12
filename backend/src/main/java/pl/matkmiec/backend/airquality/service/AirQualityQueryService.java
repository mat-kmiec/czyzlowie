package pl.matkmiec.backend.airquality.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.matkmiec.backend.airquality.dto.*;
import pl.matkmiec.backend.airquality.model.*;
import pl.matkmiec.backend.airquality.repository.*;
import pl.matkmiec.backend.weather.config.CacheConfig;
import pl.matkmiec.backend.weather.util.GeoUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AirQualityQueryService {

    private final GiosStationRepository stationRepo;
    private final GiosSensorRepository sensorRepo;
    private final GiosMeasurementRepository measurementRepo;
    private final GiosAirQualityIndexRepository indexRepo;
    private final IosAirQualityForecastRepository forecastRepo;

    @Cacheable(value = CacheConfig.CACHE_AIR_QUALITY_MAP)
    public List<AirQualityMapPointDto> getMapPoints() {
        List<GiosStation> stations = stationRepo.findAllActiveWithCoordinates();
        if (stations.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> stationIds = stations.stream().map(GiosStation::getId).toList();
        Map<Long, GiosAirQualityIndex> indexMap = indexRepo.findAllByStationIdIn(stationIds).stream()
                .collect(Collectors.toMap(idx -> idx.getStation().getId(), Function.identity(), (a, b) -> a));

        LocalDateTime since = LocalDateTime.now().minusHours(48);
        List<GiosMeasurement> recentMeasurements = measurementRepo.findRecentByStationIds(stationIds, since);

        // Group latest measurements by stationId and paramCode
        Map<Long, Map<String, GiosMeasurement>> stationParamMeasurements = new HashMap<>();
        for (GiosMeasurement m : recentMeasurements) {
            stationParamMeasurements
                    .computeIfAbsent(m.getStationId(), k -> new HashMap<>())
                    .putIfAbsent(m.getParamCode().toUpperCase(), m);
        }

        List<AirQualityMapPointDto> result = new ArrayList<>(stations.size());

        for (GiosStation station : stations) {
            GiosAirQualityIndex index = indexMap.get(station.getId());
            Map<String, GiosMeasurement> measurements = stationParamMeasurements.getOrDefault(station.getId(), Collections.emptyMap());

            Map<String, PollutantMeasurementDto> pollutants = new HashMap<>();
            for (Map.Entry<String, GiosMeasurement> entry : measurements.entrySet()) {
                String code = entry.getKey();
                GiosMeasurement m = entry.getValue();
                String indexLevel = getIndexLevelForParam(index, code);
                pollutants.put(code, new PollutantMeasurementDto(
                        code,
                        getPollutantFullName(code),
                        m.getValue(),
                        getPollutantUnit(code),
                        m.getMeasurementTime(),
                        indexLevel
                ));
            }

            Integer indexLevelId = index != null ? index.getIndexLevelId() : null;
            String indexLevelName = index != null ? index.getIndexLevelName() : null;
            String criticalParam = index != null ? index.getStIndexCrParam() : null;
            LocalDateTime lastUpdated = index != null && index.getUpdatedAt() != null ? index.getUpdatedAt() : station.getLastSyncAt();

            result.add(new AirQualityMapPointDto(
                    station.getId(),
                    station.getStationName(),
                    station.getGegrLat(),
                    station.getGegrLon(),
                    station.getAddressStreet(),
                    station.getCityName(),
                    station.getDistrictName(),
                    station.getProvinceName(),
                    station.getTerytCode(),
                    indexLevelId,
                    indexLevelName,
                    criticalParam,
                    pollutants,
                    lastUpdated
            ));
        }

        return result;
    }

    @Cacheable(value = CacheConfig.CACHE_AIR_QUALITY_RANKINGS, key = "#paramCode + '-' + #order + '-' + #limit")
    public List<AirQualityRankingDto> getRankings(String paramCode, String order, int limit) {
        String cleanParam = (paramCode != null && !paramCode.isBlank()) ? paramCode.trim().toUpperCase() : "PM10";
        boolean ascending = !"WORST".equalsIgnoreCase(order) && !"DESC".equalsIgnoreCase(order);
        int maxLimit = Math.min(Math.max(1, limit), 100);

        List<GiosStation> activeStations = stationRepo.findAllActiveWithCoordinates();
        Map<Long, GiosStation> stationMap = activeStations.stream()
                .collect(Collectors.toMap(GiosStation::getId, Function.identity(), (a, b) -> a));

        if ("AQI".equals(cleanParam) || "INDEX".equals(cleanParam)) {
            List<GiosAirQualityIndex> indexes = indexRepo.findAllActiveWithStation().stream()
                    .filter(i -> i.getIndexLevelId() != null && i.getIndexLevelId() >= 0)
                    .toList();

            List<GiosAirQualityIndex> sorted = new ArrayList<>(indexes);
            sorted.sort((a, b) -> ascending
                    ? Integer.compare(a.getIndexLevelId(), b.getIndexLevelId())
                    : Integer.compare(b.getIndexLevelId(), a.getIndexLevelId())
            );

            List<AirQualityRankingDto> rankings = new ArrayList<>();
            int rank = 1;
            for (GiosAirQualityIndex idx : sorted) {
                if (rank > maxLimit) break;
                GiosStation st = idx.getStation();
                rankings.add(new AirQualityRankingDto(
                        rank++,
                        st.getId(),
                        st.getStationName(),
                        st.getCityName(),
                        st.getDistrictName(),
                        st.getProvinceName(),
                        st.getGegrLat(),
                        st.getGegrLon(),
                        "AQI",
                        idx.getIndexLevelId() != null ? idx.getIndexLevelId().doubleValue() : null,
                        "indeks",
                        idx.getIndexLevelId(),
                        idx.getIndexLevelName(),
                        idx.getSourceDataDate() != null ? idx.getSourceDataDate() : idx.getCalculationDate()
                ));
            }
            return rankings;
        }

        LocalDateTime since = LocalDateTime.now().minusHours(48);
        List<GiosMeasurement> measurements = measurementRepo.findRecentByParamCode(cleanParam, since);

        // Deduplicate to latest measurement per station
        Map<Long, GiosMeasurement> latestByStation = new LinkedHashMap<>();
        for (GiosMeasurement m : measurements) {
            if (stationMap.containsKey(m.getStationId())) {
                latestByStation.putIfAbsent(m.getStationId(), m);
            }
        }

        List<GiosMeasurement> sorted = new ArrayList<>(latestByStation.values());
        sorted.sort((a, b) -> ascending
                ? Double.compare(a.getValue(), b.getValue())
                : Double.compare(b.getValue(), a.getValue())
        );

        List<Long> topStationIds = sorted.stream().map(GiosMeasurement::getStationId).toList();
        Map<Long, GiosAirQualityIndex> indexMap = indexRepo.findAllByStationIdIn(topStationIds).stream()
                .collect(Collectors.toMap(i -> i.getStation().getId(), Function.identity(), (a, b) -> a));

        List<AirQualityRankingDto> rankings = new ArrayList<>();
        int rank = 1;
        for (GiosMeasurement m : sorted) {
            if (rank > maxLimit) break;
            GiosStation st = stationMap.get(m.getStationId());
            if (st != null) {
                GiosAirQualityIndex idx = indexMap.get(st.getId());
                Integer indexLevelId = idx != null ? idx.getIndexLevelId() : null;
                String indexLevelName = idx != null ? getIndexLevelForParam(idx, cleanParam) : null;

                rankings.add(new AirQualityRankingDto(
                        rank++,
                        st.getId(),
                        st.getStationName(),
                        st.getCityName(),
                        st.getDistrictName(),
                        st.getProvinceName(),
                        st.getGegrLat(),
                        st.getGegrLon(),
                        cleanParam,
                        m.getValue(),
                        getPollutantUnit(cleanParam),
                        indexLevelId,
                        indexLevelName,
                        m.getMeasurementTime()
                ));
            }
        }

        return rankings;
    }

    public NearestAirQualityStationDto findNearestStation(Double lat, Double lon) {
        if (lat == null || lon == null) {
            throw new IllegalArgumentException("Współrzędne geograficzne (lat, lon) są wymagane.");
        }

        List<GiosStation> stations = stationRepo.findAllActiveWithCoordinates();
        if (stations.isEmpty()) {
            throw new NoSuchElementException("Brak aktywnych stacji pomiarowych ze współrzędnymi.");
        }

        GiosStation nearest = null;
        double minDistance = Double.MAX_VALUE;

        for (GiosStation station : stations) {
            double distance = GeoUtils.calculateDistanceKm(lat, lon, station.getGegrLat(), station.getGegrLon());
            if (distance < minDistance) {
                minDistance = distance;
                nearest = station;
            }
        }

        if (nearest == null) {
            throw new NoSuchElementException("Nie znaleziono najbliższej stacji pomiarowej.");
        }

        GiosAirQualityIndex index = indexRepo.findByStationId(nearest.getId()).orElse(null);
        LocalDateTime since = LocalDateTime.now().minusHours(48);
        List<GiosMeasurement> recentMeasurements = measurementRepo.findRecentByStationIds(List.of(nearest.getId()), since);

        Map<String, PollutantMeasurementDto> pollutants = new HashMap<>();
        for (GiosMeasurement m : recentMeasurements) {
            String code = m.getParamCode().toUpperCase();
            if (!pollutants.containsKey(code)) {
                pollutants.put(code, new PollutantMeasurementDto(
                        code,
                        getPollutantFullName(code),
                        m.getValue(),
                        getPollutantUnit(code),
                        m.getMeasurementTime(),
                        getIndexLevelForParam(index, code)
                ));
            }
        }

        return new NearestAirQualityStationDto(
                nearest,
                minDistance,
                index != null ? index.getIndexLevelId() : null,
                index != null ? index.getIndexLevelName() : null,
                index != null ? index.getStIndexCrParam() : null,
                pollutants
        );
    }

    @Cacheable(value = CacheConfig.CACHE_AIR_QUALITY_STATION_DETAILS, key = "#stationId")
    public AirQualityStationDetailsDto getStationDetails(Long stationId) {
        GiosStation station = stationRepo.findById(stationId)
                .orElseThrow(() -> new NoSuchElementException("Stacja GIOŚ o ID " + stationId + " nie istnieje."));

        GiosAirQualityIndex index = indexRepo.findByStationId(stationId).orElse(null);
        List<GiosSensor> sensors = sensorRepo.findAllByStationId(stationId);

        LocalDateTime since = LocalDateTime.now().minusHours(24);
        List<GiosMeasurement> historical = measurementRepo.findAllByStationIdAndMeasurementTimeAfterOrderByMeasurementTimeAsc(stationId, since);

        Map<String, List<HistoricalDataPointDto>> historical24h = new HashMap<>();
        Map<String, PollutantMeasurementDto> currentMeasurements = new HashMap<>();

        for (GiosMeasurement m : historical) {
            String code = m.getParamCode().toUpperCase();
            historical24h
                    .computeIfAbsent(code, k -> new ArrayList<>())
                    .add(new HistoricalDataPointDto(m.getMeasurementTime(), m.getValue()));
        }

        // For current measurements, pick the latest reading for each sensor
        for (GiosSensor sensor : sensors) {
            String code = sensor.getParamCode().toUpperCase();
            List<HistoricalDataPointDto> points = historical24h.get(code);
            if (points != null && !points.isEmpty()) {
                HistoricalDataPointDto latest = points.get(points.size() - 1);
                currentMeasurements.put(code, new PollutantMeasurementDto(
                        code,
                        sensor.getParamName(),
                        latest.value(),
                        getPollutantUnit(code),
                        latest.timestamp(),
                        getIndexLevelForParam(index, code)
                ));
            }
        }

        List<AirQualityForecastDto> forecasts = Collections.emptyList();
        if (station.getTerytCode() != null && !station.getTerytCode().isBlank()) {
            forecasts = getForecast(station.getTerytCode(), null);
        }

        return new AirQualityStationDetailsDto(
                station,
                index,
                currentMeasurements,
                historical24h,
                sensors,
                forecasts
        );
    }

    @Cacheable(value = CacheConfig.CACHE_AIR_QUALITY_FORECAST, key = "#terytCode + '-' + #pollutant")
    public List<AirQualityForecastDto> getForecast(String terytCode, String pollutant) {
        if (terytCode == null || terytCode.isBlank()) {
            return Collections.emptyList();
        }

        LocalDate today = LocalDate.now();
        List<IosAirQualityForecast> list;
        if (pollutant != null && !pollutant.isBlank()) {
            list = forecastRepo.findAllByTerytCodeAndPollutantAndForecastDateGreaterThanEqualOrderByForecastDateAsc(
                    terytCode.trim(), pollutant.trim().toUpperCase(), today
            );
        } else {
            list = forecastRepo.findAllByTerytCodeAndForecastDateGreaterThanEqualOrderByForecastDateAsc(
                    terytCode.trim(), today
            );
        }

        Map<String, List<ForecastItemDto>> grouped = list.stream()
                .collect(Collectors.groupingBy(
                        IosAirQualityForecast::getPollutant,
                        Collectors.mapping(
                                f -> new ForecastItemDto(f.getForecastDate(), f.getForecastValue(), getPollutantUnit(f.getPollutant())),
                                Collectors.toList()
                        )
                ));

        return grouped.entrySet().stream()
                .map(entry -> new AirQualityForecastDto(terytCode, entry.getKey(), entry.getValue()))
                .toList();
    }

    public List<AirQualityForecastDto> getForecastForStation(Long stationId) {
        GiosStation station = stationRepo.findById(stationId)
                .orElseThrow(() -> new NoSuchElementException("Stacja GIOŚ o ID " + stationId + " nie istnieje."));
        if (station.getTerytCode() == null || station.getTerytCode().isBlank()) {
            return Collections.emptyList();
        }
        return getForecast(station.getTerytCode(), null);
    }

    public Page<GiosMeasurement> getMeasurementHistory(Long stationId, String paramCode, LocalDateTime from, LocalDateTime to, Pageable pageable) {
        return measurementRepo.findHistory(stationId, paramCode, from, to, pageable);
    }

    @Cacheable(value = CacheConfig.CACHE_AIR_QUALITY_OVERVIEW)
    public AirQualityOverviewDto getOverview() {
        long totalStations = stationRepo.count();
        List<GiosStation> activeStations = stationRepo.findAllByIsActiveTrue();
        long activeCount = activeStations.size();

        List<GiosAirQualityIndex> indexes = indexRepo.findAll();
        Map<String, Long> byLevel = indexes.stream()
                .filter(i -> i.getIndexLevelName() != null)
                .collect(Collectors.groupingBy(GiosAirQualityIndex::getIndexLevelName, Collectors.counting()));

        List<AirQualityRankingDto> cleanest = getRankings("PM10", "BEST", 5);
        List<AirQualityRankingDto> mostPolluted = getRankings("PM10", "WORST", 5);

        return new AirQualityOverviewDto(
                totalStations,
                activeCount,
                byLevel,
                cleanest,
                mostPolluted,
                LocalDateTime.now()
        );
    }

    private String getIndexLevelForParam(GiosAirQualityIndex index, String paramCode) {
        if (index == null || paramCode == null) return null;
        return switch (paramCode.toUpperCase()) {
            case "PM10" -> index.getPm10IndexLevel();
            case "PM2.5", "PM25" -> index.getPm25IndexLevel();
            case "O3" -> index.getO3IndexLevel();
            case "NO2" -> index.getNo2IndexLevel();
            case "SO2" -> index.getSo2IndexLevel();
            case "CO" -> index.getCoIndexLevel();
            case "C6H6" -> index.getC6h6IndexLevel();
            default -> index.getIndexLevelName();
        };
    }

    private String getPollutantFullName(String paramCode) {
        if (paramCode == null) return "";
        return switch (paramCode.toUpperCase()) {
            case "PM10" -> "Pył zawieszony PM10";
            case "PM2.5", "PM25" -> "Pył zawieszony PM2.5";
            case "NO2" -> "Dwutlenek azotu";
            case "SO2" -> "Dwutlenek siarki";
            case "O3" -> "Ozon";
            case "CO" -> "Tlenek węgla";
            case "C6H6" -> "Benzen";
            default -> paramCode;
        };
    }

    private String getPollutantUnit(String paramCode) {
        if (paramCode == null) return "µg/m³";
        if ("CO".equalsIgnoreCase(paramCode)) {
            return "mg/m³";
        }
        return "µg/m³";
    }
}
