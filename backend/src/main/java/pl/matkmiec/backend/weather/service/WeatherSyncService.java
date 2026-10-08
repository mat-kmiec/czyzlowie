package pl.matkmiec.backend.weather.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.matkmiec.backend.weather.client.ImgwApiClient;
import pl.matkmiec.backend.weather.config.WeatherProperties;
import pl.matkmiec.backend.weather.dto.ImgwHydroResponseDto;
import pl.matkmiec.backend.weather.dto.ImgwMeteoResponseDto;
import pl.matkmiec.backend.weather.dto.ImgwSynopResponseDto;
import pl.matkmiec.backend.weather.mapper.ImgwDataMapper;
import pl.matkmiec.backend.weather.model.*;
import pl.matkmiec.backend.weather.repository.ImgwHydroDataRepository;
import pl.matkmiec.backend.weather.repository.ImgwMeteoDataRepository;
import pl.matkmiec.backend.weather.repository.ImgwSynopDataRepository;

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
    private final ImgwDataMapper mapper;
    private final StationManagementService stationManagementService;
    private final SyncLogService syncLogService;
    private final WeatherProperties weatherProperties;

    private final ImgwSynopDataRepository synopDataRepo;
    private final ImgwMeteoDataRepository meteoDataRepo;
    private final ImgwHydroDataRepository hydroDataRepo;

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

    public Map<String, Integer> syncAll() {
        log.info("Rozpoczynam pełną synchronizację (Synop, Meteo, Hydro)...");
        Map<String, Integer> results = new LinkedHashMap<>();
        results.put("synop", syncSynopData());
        results.put("meteo", syncMeteoData());
        results.put("hydro", syncHydroData());
        return results;
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