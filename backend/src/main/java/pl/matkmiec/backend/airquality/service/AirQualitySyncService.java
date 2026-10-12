package pl.matkmiec.backend.airquality.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.matkmiec.backend.airquality.client.GiosApiClient;
import pl.matkmiec.backend.airquality.client.IosApiClient;
import pl.matkmiec.backend.airquality.config.AirQualityProperties;
import pl.matkmiec.backend.airquality.dto.*;
import pl.matkmiec.backend.airquality.mapper.GiosDataMapper;
import pl.matkmiec.backend.airquality.mapper.IosForecastMapper;
import pl.matkmiec.backend.airquality.model.*;
import pl.matkmiec.backend.airquality.repository.GiosAirQualityIndexRepository;
import pl.matkmiec.backend.airquality.repository.GiosMeasurementRepository;
import pl.matkmiec.backend.airquality.repository.GiosSensorRepository;
import pl.matkmiec.backend.airquality.repository.GiosStationRepository;
import pl.matkmiec.backend.airquality.repository.IosAirQualityForecastRepository;
import pl.matkmiec.backend.weather.config.CacheConfig;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AirQualitySyncService {

    private final GiosApiClient giosApiClient;
    private final IosApiClient iosApiClient;
    private final GiosDataMapper giosMapper;
    private final IosForecastMapper iosMapper;
    private final AirQualityStationManagementService stationManagementService;
    private final AirQualitySyncLogService syncLogService;
    private final AirQualityProperties airQualityProperties;

    private final GiosStationRepository stationRepo;
    private final GiosSensorRepository sensorRepo;
    private final GiosMeasurementRepository measurementRepo;
    private final GiosAirQualityIndexRepository indexRepo;
    private final IosAirQualityForecastRepository forecastRepo;

    private static final List<String> FORECAST_POLLUTANTS = List.of("PM10", "NO2", "SO2", "O3");

    @Scheduled(cron = "${airquality.sync.stations-cron:0 0 3 * * *}")
    @Transactional
    @CacheEvict(value = {
            CacheConfig.CACHE_AIR_QUALITY_MAP,
            CacheConfig.CACHE_AIR_QUALITY_STATIONS,
            CacheConfig.CACHE_AIR_QUALITY_RANKINGS,
            CacheConfig.CACHE_AIR_QUALITY_STATION_DETAILS,
            CacheConfig.CACHE_AIR_QUALITY_NEAREST,
            CacheConfig.CACHE_AIR_QUALITY_OVERVIEW
    }, allEntries = true)
    public int syncStations() {
        if (!isSyncEnabled()) {
            log.info("Synchronizacja jakości powietrza jest wyłączona w konfiguracji.");
            return 0;
        }

        log.info("Rozpoczynam synchronizację stacji pomiarowych GIOŚ...");
        AirQualitySyncLog logEntry = syncLogService.logStart("GIOS_STATIONS");

        try {
            List<GiosStationResponseDto> dtos = giosApiClient.fetchAllStations();
            if (dtos == null || dtos.isEmpty()) {
                log.info("Otrzymano pustą listę stacji z GIOŚ API.");
                syncLogService.logSuccess(logEntry.getId(), 0);
                return 0;
            }

            Map<Long, GiosStation> stations = stationManagementService.autoDiscoverStations(dtos);
            int count = stations.size();
            syncLogService.logSuccess(logEntry.getId(), count);
            log.info("Pomyślnie zsynchronizowano {} stacji GIOŚ.", count);
            return count;
        } catch (Exception e) {
            log.error("Błąd podczas synchronizacji stacji GIOŚ: {}", e.getMessage(), e);
            syncLogService.logError(logEntry.getId(), e.getMessage());
            return 0;
        }
    }

    @Scheduled(cron = "${airquality.sync.sensors-cron:0 5 3 * * *}")
    @Transactional
    public int syncSensors() {
        if (!isSyncEnabled()) {
            return 0;
        }

        log.info("Rozpoczynam synchronizację stanowisk pomiarowych (czujników) GIOŚ...");
        AirQualitySyncLog logEntry = syncLogService.logStart("GIOS_SENSORS");

        try {
            List<GiosStation> activeStations = stationRepo.findAllByIsActiveTrue();
            if (activeStations.isEmpty()) {
                log.info("Brak aktywnych stacji GIOŚ do pobrania stanowisk pomiarowych.");
                syncLogService.logSuccess(logEntry.getId(), 0);
                return 0;
            }

            int concurrency = Math.max(1, airQualityProperties.sync().parallelThreads());
            Semaphore semaphore = new Semaphore(concurrency);
            List<CompletableFuture<List<GiosSensor>>> futures = new ArrayList<>();

            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                for (GiosStation station : activeStations) {
                    CompletableFuture<List<GiosSensor>> future = CompletableFuture.supplyAsync(() -> {
                        try {
                            semaphore.acquire();
                            List<GiosSensorResponseDto> sensorDtos = giosApiClient.fetchSensors(station.getId());
                            return stationManagementService.autoDiscoverSensors(station, sensorDtos);
                        } catch (Exception e) {
                            log.warn("Błąd pobierania czujników dla stacji {}: {}", station.getId(), e.getMessage());
                            return Collections.<GiosSensor>emptyList();
                        } finally {
                            semaphore.release();
                        }
                    }, executor);
                    futures.add(future);
                }

                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            }

            int totalSensors = futures.stream()
                    .map(CompletableFuture::join)
                    .mapToInt(List::size)
                    .sum();

            syncLogService.logSuccess(logEntry.getId(), totalSensors);
            log.info("Pomyślnie zsynchronizowano {} stanowisk pomiarowych dla {} stacji GIOŚ.", totalSensors, activeStations.size());
            return totalSensors;
        } catch (Exception e) {
            log.error("Błąd podczas synchronizacji stanowisk pomiarowych GIOŚ: {}", e.getMessage(), e);
            syncLogService.logError(logEntry.getId(), e.getMessage());
            return 0;
        }
    }

    @Scheduled(cron = "${airquality.sync.measurements-cron:0 10 * * * *}")
    @Transactional
    @CacheEvict(value = {
            CacheConfig.CACHE_AIR_QUALITY_MAP,
            CacheConfig.CACHE_AIR_QUALITY_RANKINGS,
            CacheConfig.CACHE_AIR_QUALITY_STATION_DETAILS,
            CacheConfig.CACHE_AIR_QUALITY_NEAREST,
            CacheConfig.CACHE_AIR_QUALITY_OVERVIEW
    }, allEntries = true)
    public int syncMeasurements() {
        if (!isSyncEnabled()) {
            return 0;
        }

        log.info("Rozpoczynam synchronizację danych pomiarowych z czujników GIOŚ...");
        AirQualitySyncLog logEntry = syncLogService.logStart("GIOS_MEASUREMENTS");

        try {
            List<GiosSensor> activeSensors = sensorRepo.findAllByIsActiveTrue();
            if (activeSensors.isEmpty()) {
                log.info("Brak aktywnych czujników GIOŚ do pobrania pomiarów.");
                syncLogService.logSuccess(logEntry.getId(), 0);
                return 0;
            }

            int concurrency = Math.max(1, airQualityProperties.sync().parallelThreads());
            Semaphore semaphore = new Semaphore(concurrency);
            List<CompletableFuture<List<GiosMeasurement>>> futures = new ArrayList<>();

            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                for (GiosSensor sensor : activeSensors) {
                    CompletableFuture<List<GiosMeasurement>> future = CompletableFuture.supplyAsync(() -> {
                        try {
                            semaphore.acquire();
                            GiosMeasurementDataResponseDto dataDto = giosApiClient.fetchMeasurementData(sensor.getId());
                            if (dataDto == null || dataDto.values() == null) {
                                return Collections.<GiosMeasurement>emptyList();
                            }

                            List<GiosMeasurement> list = new ArrayList<>();
                            for (GiosMeasurementValueDto valDto : dataDto.values()) {
                                if (valDto != null && valDto.value() != null) {
                                    GiosMeasurement entity = giosMapper.toMeasurementEntity(valDto, sensor, dataDto.key());
                                    if (entity != null) {
                                        list.add(entity);
                                    }
                                }
                            }
                            return list;
                        } catch (Exception e) {
                            log.warn("Błąd pobierania pomiarów dla czujnika {}: {}", sensor.getId(), e.getMessage());
                            return Collections.<GiosMeasurement>emptyList();
                        } finally {
                            semaphore.release();
                        }
                    }, executor);
                    futures.add(future);
                }

                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            }

            List<GiosMeasurement> allCandidates = futures.stream()
                    .map(CompletableFuture::join)
                    .flatMap(List::stream)
                    .toList();

            if (allCandidates.isEmpty()) {
                log.info("Brak nowych pomiarów GIOŚ do zapisania.");
                syncLogService.logSuccess(logEntry.getId(), 0);
                return 0;
            }

            LocalDateTime minTime = allCandidates.stream()
                    .map(GiosMeasurement::getMeasurementTime)
                    .min(LocalDateTime::compareTo)
                    .orElseGet(() -> LocalDateTime.now().minusHours(airQualityProperties.sync().deduplicationWindowHours()));

            LocalDateTime queryStartTime = minTime.minusHours(1);
            Set<String> existingKeys = measurementRepo.findAllByMeasurementTimeAfter(queryStartTime).stream()
                    .map(m -> generateMeasurementKey(m.getSensor().getId(), m.getMeasurementTime()))
                    .collect(Collectors.toSet());

            Set<String> batchUniqueKeys = new HashSet<>();
            List<GiosMeasurement> toSave = new ArrayList<>();

            for (GiosMeasurement candidate : allCandidates) {
                String key = generateMeasurementKey(candidate.getSensor().getId(), candidate.getMeasurementTime());
                if (!existingKeys.contains(key) && batchUniqueKeys.add(key)) {
                    toSave.add(candidate);
                }
            }

            saveInBatches(toSave, measurementRepo::saveAll);
            int savedCount = toSave.size();
            syncLogService.logSuccess(logEntry.getId(), savedCount);
            log.info("Pomyślnie zapisano {} nowych pomiarów GIOŚ (przeanalizowano {}).", savedCount, allCandidates.size());
            return savedCount;
        } catch (Exception e) {
            log.error("Błąd podczas synchronizacji pomiarów GIOŚ: {}", e.getMessage(), e);
            syncLogService.logError(logEntry.getId(), e.getMessage());
            return 0;
        }
    }

    @Scheduled(cron = "${airquality.sync.index-cron:0 12 * * * *}")
    @Transactional
    @CacheEvict(value = {
            CacheConfig.CACHE_AIR_QUALITY_MAP,
            CacheConfig.CACHE_AIR_QUALITY_RANKINGS,
            CacheConfig.CACHE_AIR_QUALITY_STATION_DETAILS,
            CacheConfig.CACHE_AIR_QUALITY_NEAREST,
            CacheConfig.CACHE_AIR_QUALITY_OVERVIEW
    }, allEntries = true)
    public int syncAirQualityIndexes() {
        if (!isSyncEnabled()) {
            return 0;
        }

        log.info("Rozpoczynam synchronizację indeksów jakości powietrza GIOŚ...");
        AirQualitySyncLog logEntry = syncLogService.logStart("GIOS_INDEX");

        try {
            List<GiosStation> activeStations = stationRepo.findAllByIsActiveTrue();
            if (activeStations.isEmpty()) {
                log.info("Brak aktywnych stacji GIOŚ do pobrania indeksów jakości powietrza.");
                syncLogService.logSuccess(logEntry.getId(), 0);
                return 0;
            }

            int concurrency = Math.max(1, airQualityProperties.sync().parallelThreads());
            Semaphore semaphore = new Semaphore(concurrency);
            List<CompletableFuture<GiosIndexResponseDto>> futures = new ArrayList<>();

            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                for (GiosStation station : activeStations) {
                    CompletableFuture<GiosIndexResponseDto> future = CompletableFuture.supplyAsync(() -> {
                        try {
                            semaphore.acquire();
                            return giosApiClient.fetchAirQualityIndex(station.getId());
                        } catch (Exception e) {
                            log.warn("Błąd pobierania indeksu dla stacji {}: {}", station.getId(), e.getMessage());
                            return null;
                        } finally {
                            semaphore.release();
                        }
                    }, executor);
                    futures.add(future);
                }

                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            }

            Map<Long, GiosStation> stationMap = activeStations.stream()
                    .collect(Collectors.toMap(GiosStation::getId, Function.identity(), (a, b) -> a));

            Map<Long, GiosAirQualityIndex> existingIndexes = indexRepo.findAll().stream()
                    .collect(Collectors.toMap(idx -> idx.getStation().getId(), Function.identity(), (a, b) -> a));

            List<GiosAirQualityIndex> toSave = new ArrayList<>();

            for (CompletableFuture<GiosIndexResponseDto> future : futures) {
                GiosIndexResponseDto dto = future.join();
                if (dto != null && dto.id() != null && stationMap.containsKey(dto.id())) {
                    GiosStation station = stationMap.get(dto.id());
                    GiosAirQualityIndex existing = existingIndexes.get(station.getId());

                    GiosAirQualityIndex entity = giosMapper.toIndexEntity(dto, station);
                    if (entity != null) {
                        if (existing != null) {
                            entity.setId(existing.getId());
                        }
                        toSave.add(entity);
                    }
                }
            }

            saveInBatches(toSave, indexRepo::saveAll);
            int savedCount = toSave.size();
            syncLogService.logSuccess(logEntry.getId(), savedCount);
            log.info("Pomyślnie zaktualizowano {} indeksów jakości powietrza GIOŚ.", savedCount);
            return savedCount;
        } catch (Exception e) {
            log.error("Błąd podczas synchronizacji indeksów jakości powietrza GIOŚ: {}", e.getMessage(), e);
            syncLogService.logError(logEntry.getId(), e.getMessage());
            return 0;
        }
    }

    @Scheduled(cron = "${airquality.sync.forecast-cron:0 0 */6 * * *}")
    @Transactional
    @CacheEvict(value = {
            CacheConfig.CACHE_AIR_QUALITY_FORECAST,
            CacheConfig.CACHE_AIR_QUALITY_STATION_DETAILS
    }, allEntries = true)
    public int syncForecasts() {
        if (!isSyncEnabled()) {
            return 0;
        }

        log.info("Rozpoczynam synchronizację prognoz jakości powietrza IOŚ-PIB...");
        AirQualitySyncLog logEntry = syncLogService.logStart("IOS_FORECAST");

        try {
            Set<String> terytCodes = stationRepo.findAllByIsActiveTrue().stream()
                    .map(GiosStation::getTerytCode)
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(t -> !t.isEmpty())
                    .collect(Collectors.toSet());

            if (terytCodes.isEmpty()) {
                log.info("Brak zdefiniowanych kodów TERYT dla aktywnych stacji GIOŚ do pobrania prognoz.");
                syncLogService.logSuccess(logEntry.getId(), 0);
                return 0;
            }

            int concurrency = Math.max(1, airQualityProperties.sync().parallelThreads());
            Semaphore semaphore = new Semaphore(concurrency);
            List<CompletableFuture<List<IosAirQualityForecast>>> futures = new ArrayList<>();

            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                for (String teryt : terytCodes) {
                    for (String pollutant : FORECAST_POLLUTANTS) {
                        CompletableFuture<List<IosAirQualityForecast>> future = CompletableFuture.supplyAsync(() -> {
                            try {
                                semaphore.acquire();
                                Map<String, Double> forecastMap = iosApiClient.fetchForecast(pollutant, teryt);
                                return iosMapper.toForecastEntities(teryt, pollutant, forecastMap);
                            } catch (Exception e) {
                                log.warn("Błąd pobierania prognozy IOŚ dla {} / TERYT {}: {}", pollutant, teryt, e.getMessage());
                                return Collections.<IosAirQualityForecast>emptyList();
                            } finally {
                                semaphore.release();
                            }
                        }, executor);
                        futures.add(future);
                    }
                }

                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            }

            List<IosAirQualityForecast> candidates = futures.stream()
                    .map(CompletableFuture::join)
                    .flatMap(List::stream)
                    .toList();

            if (candidates.isEmpty()) {
                log.info("Brak nowych prognoz jakości powietrza IOŚ do zapisania.");
                syncLogService.logSuccess(logEntry.getId(), 0);
                return 0;
            }

            List<IosAirQualityForecast> existingForecasts = forecastRepo.findAll();
            Map<String, IosAirQualityForecast> existingMap = existingForecasts.stream()
                    .collect(Collectors.toMap(
                            f -> generateForecastKey(f.getTerytCode(), f.getPollutant(), f.getForecastDate() != null ? f.getForecastDate().toString() : ""),
                            Function.identity(),
                            (a, b) -> a
                    ));

            List<IosAirQualityForecast> toSave = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();

            for (IosAirQualityForecast candidate : candidates) {
                String key = generateForecastKey(candidate.getTerytCode(), candidate.getPollutant(), candidate.getForecastDate() != null ? candidate.getForecastDate().toString() : "");
                IosAirQualityForecast existing = existingMap.get(key);
                if (existing != null) {
                    existing.setForecastValue(candidate.getForecastValue());
                    existing.setUpdatedAt(now);
                    toSave.add(existing);
                } else {
                    candidate.setUpdatedAt(now);
                    toSave.add(candidate);
                }
            }

            saveInBatches(toSave, forecastRepo::saveAll);
            int savedCount = toSave.size();
            syncLogService.logSuccess(logEntry.getId(), savedCount);
            log.info("Pomyślnie zsynchronizowano {} prognoz jakości powietrza IOŚ-PIB dla {} jednostek TERYT.", savedCount, terytCodes.size());
            return savedCount;
        } catch (Exception e) {
            log.error("Błąd podczas synchronizacji prognoz IOŚ-PIB: {}", e.getMessage(), e);
            syncLogService.logError(logEntry.getId(), e.getMessage());
            return 0;
        }
    }

    @Transactional
    public Map<String, Integer> syncAll() {
        log.info("Rozpoczynam pełną synchronizację pakietu jakości powietrza (stacje -> czujniki -> pomiary -> indeksy -> prognozy)...");
        Map<String, Integer> results = new LinkedHashMap<>();

        int stations = syncStations();
        results.put("GIOS_STATIONS", stations);

        int sensors = syncSensors();
        results.put("GIOS_SENSORS", sensors);

        int measurements = syncMeasurements();
        results.put("GIOS_MEASUREMENTS", measurements);

        int indexes = syncAirQualityIndexes();
        results.put("GIOS_INDEX", indexes);

        int forecasts = syncForecasts();
        results.put("IOS_FORECAST", forecasts);

        log.info("Zakończono pełną synchronizację jakości powietrza: {}", results);
        return results;
    }

    private <T> void saveInBatches(List<T> entities, Consumer<List<T>> batchConsumer) {
        if (entities == null || entities.isEmpty()) return;
        int batchSize = airQualityProperties.sync().batchSize();
        for (int i = 0; i < entities.size(); i += batchSize) {
            int end = Math.min(i + batchSize, entities.size());
            List<T> batch = entities.subList(i, end);
            batchConsumer.accept(batch);
        }
    }

    private String generateMeasurementKey(Long sensorId, LocalDateTime measurementTime) {
        return sensorId + "_" + (measurementTime != null ? measurementTime.toString() : "");
    }

    private String generateForecastKey(String teryt, String pollutant, String date) {
        return (teryt != null ? teryt.trim() : "") + "_" + (pollutant != null ? pollutant.trim().toUpperCase() : "") + "_" + (date != null ? date.trim() : "");
    }

    private boolean isSyncEnabled() {
        return airQualityProperties.sync().enabled();
    }
}
