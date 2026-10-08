package pl.matkmiec.backend.weather.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.matkmiec.backend.weather.config.CacheConfig;
import pl.matkmiec.backend.weather.dto.CurrentWeatherDto;
import pl.matkmiec.backend.weather.dto.NearestStationDto;
import pl.matkmiec.backend.weather.dto.NearestStationsOverviewDto;
import pl.matkmiec.backend.weather.model.*;
import pl.matkmiec.backend.weather.repository.*;
import pl.matkmiec.backend.weather.util.GeoUtils;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WeatherQueryService {

    private final ImgwSynopStationRepository synopStationRepo;
    private final ImgwMeteoStationRepository meteoStationRepo;
    private final ImgwHydroStationRepository hydroStationRepo;
    private final ImgwSynopDataRepository synopDataRepo;
    private final ImgwMeteoDataRepository meteoDataRepo;
    private final ImgwHydroDataRepository hydroDataRepo;
    private final OpenMeteoForecastRepository openMeteoForecastRepo;

    @Cacheable(value = CacheConfig.CACHE_SYNOP_STATIONS)
    public List<ImgwSynopStation> getActiveSynopStations() {
        return synopStationRepo.findAllByIsActiveTrue();
    }

    @Cacheable(value = CacheConfig.CACHE_METEO_STATIONS)
    public List<ImgwMeteoStation> getActiveMeteoStations() {
        return meteoStationRepo.findAllByIsActiveTrue();
    }

    @Cacheable(value = CacheConfig.CACHE_HYDRO_STATIONS)
    public List<ImgwHydroStation> getActiveHydroStations() {
        return hydroStationRepo.findAllByIsActiveTrue();
    }

    @Cacheable(value = CacheConfig.CACHE_FORECAST, key = "#stationId")
    public List<OpenMeteoForecast> getForecastForStation(String stationId) {
        return openMeteoForecastRepo.findAllBySynopStation_IdAndForecastTimeAfterOrderByForecastTimeAsc(
                stationId,
                LocalDateTime.now().minusHours(1)
        );
    }

    @Cacheable(value = CacheConfig.CACHE_NEAREST_STATION, key = "#lat + '_' + #lon + '_' + #stationType")
    public NearestStationDto findNearestStation(Double lat, Double lon, String stationType) {
        if (lat == null || lon == null) {
            throw new IllegalArgumentException("Współrzędne lat i lon są wymagane.");
        }

        String type = stationType != null ? stationType.toUpperCase().trim() : "ALL";
        List<NearestStationDto> candidates = new ArrayList<>();

        if ("SYNOP".equals(type) || "ALL".equals(type)) {
            findNearestSynopStation(lat, lon).ifPresent(candidates::add);
        }
        if ("METEO".equals(type) || "ALL".equals(type)) {
            findNearestMeteoStation(lat, lon).ifPresent(candidates::add);
        }
        if ("HYDRO".equals(type) || "ALL".equals(type)) {
            findNearestHydroStation(lat, lon).ifPresent(candidates::add);
        }

        return candidates.stream()
                .min(Comparator.comparingDouble(NearestStationDto::getDistanceKm))
                .orElseThrow(() -> new NoSuchElementException("Nie znaleziono żadnej aktywnej stacji w pobliżu współrzędnych: " + lat + ", " + lon));
    }

    @Cacheable(value = CacheConfig.CACHE_NEAREST_STATION, key = "'overview_' + #lat + '_' + #lon")
    public NearestStationsOverviewDto findNearestStationsOverview(Double lat, Double lon) {
        if (lat == null || lon == null) {
            throw new IllegalArgumentException("Współrzędne lat i lon są wymagane.");
        }

        return NearestStationsOverviewDto.builder()
                .nearestSynop(findNearestSynopStation(lat, lon).orElse(null))
                .nearestMeteo(findNearestMeteoStation(lat, lon).orElse(null))
                .nearestHydro(findNearestHydroStation(lat, lon).orElse(null))
                .build();
    }

    public Optional<NearestStationDto> findNearestSynopStation(double lat, double lon) {
        return synopStationRepo.findAllByIsActiveTrue().stream()
                .filter(s -> s.getLat() != null && s.getLon() != null)
                .map(s -> NearestStationDto.builder()
                        .id(s.getId())
                        .name(s.getName())
                        .stationType("SYNOP")
                        .lat(s.getLat())
                        .lon(s.getLon())
                        .distanceKm(GeoUtils.calculateDistanceKm(lat, lon, s.getLat(), s.getLon()))
                        .build())
                .min(Comparator.comparingDouble(NearestStationDto::getDistanceKm));
    }

    public Optional<NearestStationDto> findNearestMeteoStation(double lat, double lon) {
        return meteoStationRepo.findAllByIsActiveTrue().stream()
                .filter(s -> s.getLat() != null && s.getLon() != null)
                .map(s -> NearestStationDto.builder()
                        .id(s.getId())
                        .name(s.getName())
                        .stationType("METEO")
                        .lat(s.getLat())
                        .lon(s.getLon())
                        .distanceKm(GeoUtils.calculateDistanceKm(lat, lon, s.getLat(), s.getLon()))
                        .build())
                .min(Comparator.comparingDouble(NearestStationDto::getDistanceKm));
    }

    public Optional<NearestStationDto> findNearestHydroStation(double lat, double lon) {
        return hydroStationRepo.findAllByIsActiveTrue().stream()
                .filter(s -> s.getLat() != null && s.getLon() != null)
                .map(s -> NearestStationDto.builder()
                        .id(s.getId())
                        .name(s.getName())
                        .stationType("HYDRO")
                        .lat(s.getLat())
                        .lon(s.getLon())
                        .distanceKm(GeoUtils.calculateDistanceKm(lat, lon, s.getLat(), s.getLon()))
                        .build())
                .min(Comparator.comparingDouble(NearestStationDto::getDistanceKm));
    }

    @Cacheable(value = CacheConfig.CACHE_CURRENT_WEATHER, key = "#stationId")
    public CurrentWeatherDto getCurrentWeatherByStationId(String stationId) {
        ImgwSynopStation station = synopStationRepo.findById(stationId)
                .orElseThrow(() -> new NoSuchElementException("Nie znaleziono stacji synoptycznej o ID: " + stationId));

        Optional<ImgwSynopData> latestSynopOpt = synopDataRepo.findFirstByStation_IdOrderByMeasurementTimeDesc(stationId);
        List<OpenMeteoForecast> upcomingForecast = openMeteoForecastRepo
                .findAllBySynopStation_IdAndForecastTimeAfterOrderByForecastTimeAsc(
                        stationId,
                        LocalDateTime.now().minusHours(1)
                );

        ImgwSynopData synop = latestSynopOpt.orElse(null);

        return CurrentWeatherDto.builder()
                .stationId(station.getId())
                .stationName(station.getName())
                .lat(station.getLat())
                .lon(station.getLon())
                .measurementTime(synop != null ? synop.getMeasurementTime() : null)
                .temperature(synop != null ? synop.getTemperature() : null)
                .windSpeed(synop != null ? synop.getWindSpeed() : null)
                .windDirection(synop != null ? synop.getWindDirection() : null)
                .relativeHumidity(synop != null ? synop.getRelativeHumidity() : null)
                .totalPrecipitation(synop != null ? synop.getPrecipitation() : null)
                .pressure(synop != null ? synop.getPressure() : null)
                .synopData(synop)
                .upcomingForecast(upcomingForecast)
                .build();
    }

    @Cacheable(value = CacheConfig.CACHE_CURRENT_WEATHER, key = "'coords_' + #lat + '_' + #lon")
    public CurrentWeatherDto getCurrentWeatherByCoordinates(Double lat, Double lon) {
        if (lat == null || lon == null) {
            throw new IllegalArgumentException("Współrzędne lat i lon są wymagane.");
        }

        NearestStationDto nearestSynop = findNearestSynopStation(lat, lon)
                .orElseThrow(() -> new NoSuchElementException("Nie znaleziono żadnej aktywnej stacji synoptycznej ze współrzędnymi."));

        CurrentWeatherDto weather = getCurrentWeatherByStationId(nearestSynop.getId());

        return CurrentWeatherDto.builder()
                .stationId(weather.getStationId())
                .stationName(weather.getStationName())
                .lat(weather.getLat())
                .lon(weather.getLon())
                .distanceKm(nearestSynop.getDistanceKm())
                .measurementTime(weather.getMeasurementTime())
                .temperature(weather.getTemperature())
                .windSpeed(weather.getWindSpeed())
                .windDirection(weather.getWindDirection())
                .relativeHumidity(weather.getRelativeHumidity())
                .totalPrecipitation(weather.getTotalPrecipitation())
                .pressure(weather.getPressure())
                .synopData(weather.getSynopData())
                .upcomingForecast(weather.getUpcomingForecast())
                .build();
    }

    public Page<ImgwSynopData> getSynopHistory(String stationId, LocalDateTime from, LocalDateTime to, Pageable pageable) {
        if (stationId != null && !stationId.isBlank() && from != null && to != null) {
            return synopDataRepo.findAllByStation_IdAndMeasurementTimeBetween(stationId.trim(), from, to, pageable);
        } else if (stationId != null && !stationId.isBlank()) {
            return synopDataRepo.findAllByStation_Id(stationId.trim(), pageable);
        } else if (from != null && to != null) {
            return synopDataRepo.findAllByMeasurementTimeBetween(from, to, pageable);
        } else {
            return synopDataRepo.findAll(pageable);
        }
    }

    public Page<ImgwMeteoData> getMeteoHistory(String stationId, LocalDateTime from, LocalDateTime to, Pageable pageable) {
        if (stationId != null && !stationId.isBlank() && from != null && to != null) {
            return meteoDataRepo.findAllByStation_IdAndMeasurementTimeBetween(stationId.trim(), from, to, pageable);
        } else if (stationId != null && !stationId.isBlank()) {
            return meteoDataRepo.findAllByStation_Id(stationId.trim(), pageable);
        } else if (from != null && to != null) {
            return meteoDataRepo.findAllByMeasurementTimeBetween(from, to, pageable);
        } else {
            return meteoDataRepo.findAll(pageable);
        }
    }

    public Page<ImgwHydroData> getHydroHistory(String stationId, LocalDateTime from, LocalDateTime to, Pageable pageable) {
        if (stationId != null && !stationId.isBlank() && from != null && to != null) {
            return hydroDataRepo.findAllByStation_IdAndMeasurementTimeBetween(stationId.trim(), from, to, pageable);
        } else if (stationId != null && !stationId.isBlank()) {
            return hydroDataRepo.findAllByStation_Id(stationId.trim(), pageable);
        } else if (from != null && to != null) {
            return hydroDataRepo.findAllByMeasurementTimeBetween(from, to, pageable);
        } else {
            return hydroDataRepo.findAll(pageable);
        }
    }
}
