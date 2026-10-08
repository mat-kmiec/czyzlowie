package pl.matkmiec.backend.weather.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.matkmiec.backend.weather.client.ImgwApiClient;
import pl.matkmiec.backend.weather.client.OpenMeteoApiClient;
import pl.matkmiec.backend.weather.config.WeatherProperties;
import pl.matkmiec.backend.weather.dto.ImgwHydroResponseDto;
import pl.matkmiec.backend.weather.dto.ImgwMeteoResponseDto;
import pl.matkmiec.backend.weather.dto.ImgwSynopResponseDto;
import pl.matkmiec.backend.weather.dto.OpenMeteoResponseDto;
import pl.matkmiec.backend.weather.mapper.ImgwDataMapper;
import pl.matkmiec.backend.weather.mapper.OpenMeteoDataMapper;
import pl.matkmiec.backend.weather.model.*;
import pl.matkmiec.backend.weather.repository.ImgwHydroDataRepository;
import pl.matkmiec.backend.weather.repository.ImgwMeteoDataRepository;
import pl.matkmiec.backend.weather.repository.ImgwSynopDataRepository;
import pl.matkmiec.backend.weather.repository.ImgwSynopStationRepository;
import pl.matkmiec.backend.weather.repository.OpenMeteoForecastRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherSyncService {

    private final ImgwApiClient imgwApiClient;
    private final OpenMeteoApiClient openMeteoApiClient;
    private final ImgwDataMapper mapper;
    private final OpenMeteoDataMapper openMeteoMapper;
    private final StationManagementService stationManagementService;
    private final SyncLogService syncLogService;
    private final WeatherProperties weatherProperties;

    private final ImgwSynopDataRepository synopDataRepo;
    private final ImgwMeteoDataRepository meteoDataRepo;
    private final ImgwHydroDataRepository hydroDataRepo;
    private final ImgwSynopStationRepository synopStationRepo;
    private final OpenMeteoForecastRepository openMeteoForecastRepo;

    @Scheduled(cron = "${weather.sync.synop-cron:0 15 * * * *}")
    @Transactional
    public int syncSynopData() {
        if (!isSyncEnabled()) {
            log.info("Synchronizacja pogodowa jest wyłączona w konfiguracji.");
            return 0;
        }

        log.info("Rozpoczynam synchronizację danych IMGW Synop...");
        WeatherSyncLog logEntry = syncLogService.logStart("IMGW_SYNOP");

        try {
            List<ImgwSynopResponseDto> dtos = imgwApiClient.fetchSynop();
            if (dtos == null || dtos.isEmpty()) {
                log.info("Otrzymano pustą listę rekordów Synop z IMGW.");
                syncLogService.logSuccess(logEntry.getId(), 0);
                return 0;
            }

            Map<String, ImgwSynopStation> allStations = stationManagementService.autoDiscoverSynopStations(dtos);

            Map<String, ImgwSynopStation> activeStations = allStations.values().stream()
                    .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
                    .collect(Collectors.toMap(ImgwSynopStation::getId, Function.identity(), (a, b) -> a));

            Set<String> batchUniqueKeys = new HashSet<>();
            List<ImgwSynopData> candidates = new ArrayList<>();

            for (ImgwSynopResponseDto dto : dtos) {
                if (dto.idStacji() != null && activeStations.containsKey(dto.idStacji().trim())) {
                    ImgwSynopStation station = activeStations.get(dto.idStacji().trim());
                    ImgwSynopData entity = mapper.toSynopDataEntity(dto, station);
                    if (entity != null && entity.getMeasurementTime() != null) {
                        String key = generateUniqueKey(entity.getStation().getId(), entity.getMeasurementTime());
                        if (batchUniqueKeys.add(key)) {
                            candidates.add(entity);
                        }
                    }
                }
            }

            if (candidates.isEmpty()) {
                log.info("Brak nowych lub aktywnych pomiarów Synop do przetworzenia.");
                syncLogService.logSuccess(logEntry.getId(), 0);
                return 0;
            }

            LocalDateTime minMeasurementTime = candidates.stream()
                    .map(ImgwSynopData::getMeasurementTime)
                    .min(LocalDateTime::compareTo)
                    .orElseGet(() -> LocalDateTime.now().minusHours(weatherProperties.sync().deduplicationWindowHours()));

            LocalDateTime queryStartTime = minMeasurementTime.minusHours(1);
            Set<String> existingKeys = synopDataRepo.findAllByMeasurementTimeAfter(queryStartTime).stream()
                    .map(data -> generateUniqueKey(data.getStation().getId(), data.getMeasurementTime()))
                    .collect(Collectors.toSet());

            List<ImgwSynopData> toSave = candidates.stream()
                    .filter(data -> !existingKeys.contains(generateUniqueKey(data.getStation().getId(), data.getMeasurementTime())))
                    .toList();

            if (!toSave.isEmpty()) {
                int batchSize = weatherProperties.sync().batchSize();
                saveInBatches(toSave, synopDataRepo::saveAll, batchSize);
                log.info("Pomyślnie zapisano {} nowych rekordów Synop.", toSave.size());
            } else {
                log.info("Wszystkie rekordy Synop z bieżącej odpowiedzi są już zapisane w bazie.");
            }

            syncLogService.logSuccess(logEntry.getId(), toSave.size());
            return toSave.size();

        } catch (Exception e) {
            log.error("Błąd podczas synchronizacji IMGW Synop: {}", e.getMessage(), e);
            syncLogService.logError(logEntry.getId(), e.getMessage());
            throw e;
        }
    }

    @Scheduled(cron = "${weather.sync.meteo-cron:0 */10 * * * *}")
    @Transactional
    public int syncMeteoData() {
        if (!isSyncEnabled()) {
            log.info("Synchronizacja pogodowa jest wyłączona w konfiguracji.");
            return 0;
        }

        log.info("Rozpoczynam synchronizację danych IMGW Meteo...");
        WeatherSyncLog logEntry = syncLogService.logStart("IMGW_METEO");

        try {
            List<ImgwMeteoResponseDto> dtos = imgwApiClient.fetchMeteo();
            if (dtos == null || dtos.isEmpty()) {
                log.info("Otrzymano pustą listę rekordów Meteo z IMGW.");
                syncLogService.logSuccess(logEntry.getId(), 0);
                return 0;
            }

            Map<String, ImgwMeteoStation> allStations = stationManagementService.autoDiscoverMeteoStations(dtos);

            Map<String, ImgwMeteoStation> activeStations = allStations.values().stream()
                    .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
                    .collect(Collectors.toMap(ImgwMeteoStation::getId, Function.identity(), (a, b) -> a));

            Set<String> batchUniqueKeys = new HashSet<>();
            List<ImgwMeteoData> candidates = new ArrayList<>();

            for (ImgwMeteoResponseDto dto : dtos) {
                if (dto.kodStacji() != null && activeStations.containsKey(dto.kodStacji().trim())) {
                    ImgwMeteoStation station = activeStations.get(dto.kodStacji().trim());
                    ImgwMeteoData entity = mapper.toMeteoDataEntity(dto, station);
                    if (entity != null && entity.getMeasurementTime() != null) {
                        String key = generateUniqueKey(entity.getStation().getId(), entity.getMeasurementTime());
                        if (batchUniqueKeys.add(key)) {
                            candidates.add(entity);
                        }
                    }
                }
            }

            if (candidates.isEmpty()) {
                log.info("Brak nowych lub aktywnych pomiarów Meteo do przetworzenia.");
                syncLogService.logSuccess(logEntry.getId(), 0);
                return 0;
            }

            LocalDateTime minMeasurementTime = candidates.stream()
                    .map(ImgwMeteoData::getMeasurementTime)
                    .min(LocalDateTime::compareTo)
                    .orElseGet(() -> LocalDateTime.now().minusHours(weatherProperties.sync().deduplicationWindowHours()));

            LocalDateTime queryStartTime = minMeasurementTime.minusHours(1);
            Set<String> existingKeys = meteoDataRepo.findAllByMeasurementTimeAfter(queryStartTime).stream()
                    .map(data -> generateUniqueKey(data.getStation().getId(), data.getMeasurementTime()))
                    .collect(Collectors.toSet());

            List<ImgwMeteoData> toSave = candidates.stream()
                    .filter(data -> !existingKeys.contains(generateUniqueKey(data.getStation().getId(), data.getMeasurementTime())))
                    .toList();

            if (!toSave.isEmpty()) {
                int batchSize = weatherProperties.sync().batchSize();
                saveInBatches(toSave, meteoDataRepo::saveAll, batchSize);
                log.info("Pomyślnie zapisano {} nowych rekordów Meteo.", toSave.size());
            } else {
                log.info("Wszystkie rekordy Meteo z bieżącej odpowiedzi są już zapisane w bazie.");
            }

            syncLogService.logSuccess(logEntry.getId(), toSave.size());
            return toSave.size();

        } catch (Exception e) {
            log.error("Błąd podczas synchronizacji IMGW Meteo: {}", e.getMessage(), e);
            syncLogService.logError(logEntry.getId(), e.getMessage());
            throw e;
        }
    }

    @Scheduled(cron = "${weather.sync.hydro-cron:0 */10 * * * *}")
    @Transactional
    public int syncHydroData() {
        if (!isSyncEnabled()) {
            log.info("Synchronizacja pogodowa jest wyłączona w konfiguracji.");
            return 0;
        }

        log.info("Rozpoczynam synchronizację danych IMGW Hydro...");
        WeatherSyncLog logEntry = syncLogService.logStart("IMGW_HYDRO");

        try {
            List<ImgwHydroResponseDto> dtos = imgwApiClient.fetchHydro();
            if (dtos == null || dtos.isEmpty()) {
                log.info("Otrzymano pustą listę rekordów Hydro z IMGW.");
                syncLogService.logSuccess(logEntry.getId(), 0);
                return 0;
            }

            Map<String, ImgwHydroStation> allStations = stationManagementService.autoDiscoverHydroStations(dtos);

            Map<String, ImgwHydroStation> activeStations = allStations.values().stream()
                    .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
                    .collect(Collectors.toMap(ImgwHydroStation::getId, Function.identity(), (a, b) -> a));

            Set<String> batchUniqueKeys = new HashSet<>();
            List<ImgwHydroData> candidates = new ArrayList<>();

            for (ImgwHydroResponseDto dto : dtos) {
                if (dto.idStacji() != null && activeStations.containsKey(dto.idStacji().trim())) {
                    ImgwHydroStation station = activeStations.get(dto.idStacji().trim());
                    ImgwHydroData entity = mapper.toHydroDataEntity(dto, station);
                    if (entity != null && entity.getMeasurementTime() != null) {
                        String key = generateUniqueKey(entity.getStation().getId(), entity.getMeasurementTime());
                        if (batchUniqueKeys.add(key)) {
                            candidates.add(entity);
                        }
                    }
                }
            }

            if (candidates.isEmpty()) {
                log.info("Brak nowych lub aktywnych pomiarów Hydro do przetworzenia.");
                syncLogService.logSuccess(logEntry.getId(), 0);
                return 0;
            }

            LocalDateTime minMeasurementTime = candidates.stream()
                    .map(ImgwHydroData::getMeasurementTime)
                    .min(LocalDateTime::compareTo)
                    .orElseGet(() -> LocalDateTime.now().minusHours(weatherProperties.sync().deduplicationWindowHours()));

            LocalDateTime queryStartTime = minMeasurementTime.minusHours(1);
            Set<String> existingKeys = hydroDataRepo.findAllByMeasurementTimeAfter(queryStartTime).stream()
                    .map(data -> generateUniqueKey(data.getStation().getId(), data.getMeasurementTime()))
                    .collect(Collectors.toSet());

            List<ImgwHydroData> toSave = candidates.stream()
                    .filter(data -> !existingKeys.contains(generateUniqueKey(data.getStation().getId(), data.getMeasurementTime())))
                    .toList();

            if (!toSave.isEmpty()) {
                int batchSize = weatherProperties.sync().batchSize();
                saveInBatches(toSave, hydroDataRepo::saveAll, batchSize);
                log.info("Pomyślnie zapisano {} nowych rekordów Hydro.", toSave.size());
            } else {
                log.info("Wszystkie rekordy Hydro z bieżącej odpowiedzi są już zapisane w bazie.");
            }

            syncLogService.logSuccess(logEntry.getId(), toSave.size());
            return toSave.size();

        } catch (Exception e) {
            log.error("Błąd podczas synchronizacji IMGW Hydro: {}", e.getMessage(), e);
            syncLogService.logError(logEntry.getId(), e.getMessage());
            throw e;
        }
    }

    @Scheduled(cron = "${weather.sync.open-meteo-cron:0 0 */6 * * *}")
    @Transactional
    public int syncOpenMeteoData() {
        if (!isSyncEnabled()) {
            log.info("Synchronizacja pogodowa jest wyłączona w konfiguracji.");
            return 0;
        }

        log.info("Rozpoczynam synchronizację danych prognozy Open-Meteo...");
        WeatherSyncLog logEntry = syncLogService.logStart("OPEN_METEO");

        try {
            stationManagementService.populateMissingSynopCoordinates();

            List<ImgwSynopStation> activeStations = synopStationRepo.findAllByIsActiveTrue().stream()
                    .filter(s -> s.getLat() != null && s.getLon() != null)
                    .toList();

            if (activeStations.isEmpty()) {
                log.info("Brak aktywnych stacji Synop ze współrzędnymi do synchronizacji Open-Meteo.");
                syncLogService.logSuccess(logEntry.getId(), 0);
                return 0;
            }

            int pastDays = weatherProperties.sync() != null ? weatherProperties.sync().openMeteoPastDays() : 1;
            int forecastDays = weatherProperties.sync() != null ? weatherProperties.sync().openMeteoForecastDays() : 6;
            LocalDateTime now = LocalDateTime.now();

            List<OpenMeteoForecast> allCandidates = new ArrayList<>();

            for (ImgwSynopStation station : activeStations) {
                try {
                    OpenMeteoResponseDto dto = openMeteoApiClient.fetchForecast(
                            station.getLat(),
                            station.getLon(),
                            pastDays,
                            forecastDays
                    );
                    if (dto != null) {
                        List<OpenMeteoForecast> stationForecasts = openMeteoMapper.toForecastEntities(dto, station, now);
                        allCandidates.addAll(stationForecasts);
                    }
                } catch (Exception e) {
                    log.error("Nie udało się pobrać prognozy Open-Meteo dla stacji {} ({}): {}",
                            station.getId(), station.getName(), e.getMessage());
                }
            }

            if (allCandidates.isEmpty()) {
                log.info("Brak danych prognozy Open-Meteo do przetworzenia.");
                syncLogService.logSuccess(logEntry.getId(), 0);
                return 0;
            }

            LocalDateTime minTime = allCandidates.stream()
                    .map(OpenMeteoForecast::getForecastTime)
                    .min(LocalDateTime::compareTo)
                    .orElse(now.minusDays(1));
            LocalDateTime maxTime = allCandidates.stream()
                    .map(OpenMeteoForecast::getForecastTime)
                    .max(LocalDateTime::compareTo)
                    .orElse(now.plusDays(6));

            List<String> stationIds = activeStations.stream().map(ImgwSynopStation::getId).toList();
            List<OpenMeteoForecast> existingRecords = openMeteoForecastRepo
                    .findAllBySynopStation_IdInAndForecastTimeBetween(stationIds, minTime, maxTime);

            Map<String, OpenMeteoForecast> existingMap = existingRecords.stream()
                    .collect(Collectors.toMap(
                            f -> generateUniqueKey(f.getSynopStation().getId(), f.getForecastTime()),
                            Function.identity(),
                            (a, b) -> a
                    ));

            Set<String> processedKeys = new HashSet<>();
            List<OpenMeteoForecast> toSave = new ArrayList<>();

            for (OpenMeteoForecast candidate : allCandidates) {
                String key = generateUniqueKey(candidate.getSynopStation().getId(), candidate.getForecastTime());
                if (!processedKeys.add(key)) {
                    continue;
                }

                OpenMeteoForecast existing = existingMap.get(key);
                if (existing == null) {
                    toSave.add(candidate);
                } else if (hasForecastChanged(existing, candidate)) {
                    existing.setTemperature(candidate.getTemperature());
                    existing.setWindSpeed(candidate.getWindSpeed());
                    existing.setWindDirection(candidate.getWindDirection());
                    existing.setRelativeHumidity(candidate.getRelativeHumidity());
                    existing.setPrecipitation(candidate.getPrecipitation());
                    existing.setPressure(candidate.getPressure());
                    existing.setGeneratedAt(candidate.getGeneratedAt());
                    toSave.add(existing);
                }
            }

            if (!toSave.isEmpty()) {
                int batchSize = weatherProperties.sync().batchSize();
                saveInBatches(toSave, openMeteoForecastRepo::saveAll, batchSize);
                log.info("Pomyślnie zapisano/zaktualizowano {} rekordów Open-Meteo.", toSave.size());
            } else {
                log.info("Wszystkie rekordy prognozy Open-Meteo są aktualne i nie wymagają zmian.");
            }

            syncLogService.logSuccess(logEntry.getId(), toSave.size());
            return toSave.size();

        } catch (Exception e) {
            log.error("Błąd podczas synchronizacji Open-Meteo: {}", e.getMessage(), e);
            syncLogService.logError(logEntry.getId(), e.getMessage());
            throw e;
        }
    }

    public Map<String, Integer> syncAll() {
        log.info("Rozpoczynam pełną synchronizację (Synop, Meteo, Hydro, Open-Meteo)...");
        Map<String, Integer> results = new LinkedHashMap<>();
        results.put("synop", syncSynopData());
        results.put("meteo", syncMeteoData());
        results.put("hydro", syncHydroData());
        results.put("openMeteo", syncOpenMeteoData());
        return results;
    }

    private boolean hasForecastChanged(OpenMeteoForecast existing, OpenMeteoForecast candidate) {
        return !doubleEquals(existing.getTemperature(), candidate.getTemperature())
                || !doubleEquals(existing.getWindSpeed(), candidate.getWindSpeed())
                || !Objects.equals(existing.getWindDirection(), candidate.getWindDirection())
                || !doubleEquals(existing.getRelativeHumidity(), candidate.getRelativeHumidity())
                || !doubleEquals(existing.getPrecipitation(), candidate.getPrecipitation())
                || !doubleEquals(existing.getPressure(), candidate.getPressure());
    }

    private boolean doubleEquals(Double a, Double b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return Math.abs(a - b) < 0.0001;
    }

    private boolean isSyncEnabled() {
        return weatherProperties.sync() == null || weatherProperties.sync().enabled();
    }

    private <T> void saveInBatches(List<T> items, Consumer<List<T>> batchConsumer, int batchSize) {
        if (items == null || items.isEmpty()) return;
        int effectiveBatchSize = batchSize > 0 ? batchSize : 50;
        for (int i = 0; i < items.size(); i += effectiveBatchSize) {
            int end = Math.min(i + effectiveBatchSize, items.size());
            List<T> batch = items.subList(i, end);
            batchConsumer.accept(batch);
        }
    }

    private String generateUniqueKey(String stationId, LocalDateTime measurementTime) {
        return stationId + "#" + measurementTime.toString();
    }
}
