package pl.matkmiec.backend.weather.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.matkmiec.backend.weather.config.CacheConfig;
import pl.matkmiec.backend.weather.model.ImgwHydroStation;
import pl.matkmiec.backend.weather.model.ImgwMeteoStation;
import pl.matkmiec.backend.weather.model.ImgwSynopStation;
import pl.matkmiec.backend.weather.model.WeatherSyncLog;
import pl.matkmiec.backend.weather.service.StationManagementService;
import pl.matkmiec.backend.weather.service.SyncLogService;
import pl.matkmiec.backend.weather.service.WeatherCleanupService;
import pl.matkmiec.backend.weather.service.WeatherSyncService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/weather")
@RequiredArgsConstructor
@Tag(name = "Weather Admin", description = "Endpoints for administering weather synchronization and station lifecycle")
public class WeatherAdminController {

    private final WeatherSyncService weatherSyncService;
    private final StationManagementService stationManagementService;
    private final SyncLogService syncLogService;
    private final WeatherCleanupService weatherCleanupService;

    @PostMapping("/sync/synop")
    @Operation(summary = "Manually trigger IMGW Synop synchronization")
    public ResponseEntity<Map<String, Object>> triggerSynopSync() {
        int records = weatherSyncService.syncSynopData();
        return ResponseEntity.ok(Map.of("provider", "IMGW_SYNOP", "recordsProcessed", records));
    }

    @PostMapping("/sync/meteo")
    @Operation(summary = "Manually trigger IMGW Meteo synchronization")
    public ResponseEntity<Map<String, Object>> triggerMeteoSync() {
        int records = weatherSyncService.syncMeteoData();
        return ResponseEntity.ok(Map.of("provider", "IMGW_METEO", "recordsProcessed", records));
    }

    @PostMapping("/sync/hydro")
    @Operation(summary = "Manually trigger IMGW Hydro synchronization")
    public ResponseEntity<Map<String, Object>> triggerHydroSync() {
        int records = weatherSyncService.syncHydroData();
        return ResponseEntity.ok(Map.of("provider", "IMGW_HYDRO", "recordsProcessed", records));
    }

    @PostMapping("/sync/open-meteo")
    @Operation(summary = "Manually trigger Open-Meteo forecast synchronization")
    public ResponseEntity<Map<String, Object>> triggerOpenMeteoSync() {
        int records = weatherSyncService.syncOpenMeteoData();
        return ResponseEntity.ok(Map.of("provider", "OPEN_METEO", "recordsProcessed", records));
    }

    @PostMapping("/sync/all")
    @Operation(summary = "Manually trigger synchronization for all weather providers")
    public ResponseEntity<Map<String, Object>> triggerAllSync() {
        Map<String, Integer> results = weatherSyncService.syncAll();
        return ResponseEntity.ok(Map.of("status", "SUCCESS", "results", results));
    }

    @PostMapping("/cleanup")
    @Operation(summary = "Manually trigger weather data cleanup")
    public ResponseEntity<Map<String, Object>> triggerCleanup(
            @RequestParam(required = false) Integer retentionDays
    ) {
        Map<String, Object> result = retentionDays != null
                ? weatherCleanupService.cleanupDataOlderThan(retentionDays)
                : weatherCleanupService.cleanupOldData();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/sync/logs")
    @Operation(summary = "Fetch synchronization logs with pagination")
    public ResponseEntity<Page<WeatherSyncLog>> getSyncLogs(
            @ParameterObject @PageableDefault(sort = "startedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(syncLogService.getLogs(pageable));
    }

    @GetMapping("/stations/synop")
    @Operation(summary = "Get all Synop stations")
    public ResponseEntity<List<ImgwSynopStation>> getAllSynopStations() {
        return ResponseEntity.ok(stationManagementService.getAllSynopStations());
    }

    @GetMapping("/stations/meteo")
    @Operation(summary = "Get all Meteo stations")
    public ResponseEntity<List<ImgwMeteoStation>> getAllMeteoStations() {
        return ResponseEntity.ok(stationManagementService.getAllMeteoStations());
    }

    @GetMapping("/stations/hydro")
    @Operation(summary = "Get all Hydro stations")
    public ResponseEntity<List<ImgwHydroStation>> getAllHydroStations() {
        return ResponseEntity.ok(stationManagementService.getAllHydroStations());
    }

    @PatchMapping("/stations/synop/{id}/toggle-active")
    @Operation(summary = "Toggle active status for a Synop station")
    @CacheEvict(value = {CacheConfig.CACHE_SYNOP_STATIONS, CacheConfig.CACHE_CURRENT_WEATHER, CacheConfig.CACHE_NEAREST_STATION}, allEntries = true)
    public ResponseEntity<Map<String, Object>> toggleSynopStation(@PathVariable String id) {
        boolean active = stationManagementService.toggleSynopStationActive(id);
        return ResponseEntity.ok(Map.of("stationId", id, "isActive", active));
    }

    @PatchMapping("/stations/meteo/{id}/toggle-active")
    @Operation(summary = "Toggle active status for a Meteo station")
    @CacheEvict(value = {CacheConfig.CACHE_METEO_STATIONS, CacheConfig.CACHE_NEAREST_STATION}, allEntries = true)
    public ResponseEntity<Map<String, Object>> toggleMeteoStation(@PathVariable String id) {
        boolean active = stationManagementService.toggleMeteoStationActive(id);
        return ResponseEntity.ok(Map.of("stationId", id, "isActive", active));
    }

    @PatchMapping("/stations/hydro/{id}/toggle-active")
    @Operation(summary = "Toggle active status for a Hydro station")
    @CacheEvict(value = {CacheConfig.CACHE_HYDRO_STATIONS, CacheConfig.CACHE_NEAREST_STATION}, allEntries = true)
    public ResponseEntity<Map<String, Object>> toggleHydroStation(@PathVariable String id) {
        boolean active = stationManagementService.toggleHydroStationActive(id);
        return ResponseEntity.ok(Map.of("stationId", id, "isActive", active));
    }
}
