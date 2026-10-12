package pl.matkmiec.backend.airquality.controller;

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
import pl.matkmiec.backend.airquality.model.AirQualitySyncLog;
import pl.matkmiec.backend.airquality.model.GiosStation;
import pl.matkmiec.backend.airquality.service.AirQualityCleanupService;
import pl.matkmiec.backend.airquality.service.AirQualityStationManagementService;
import pl.matkmiec.backend.airquality.service.AirQualitySyncLogService;
import pl.matkmiec.backend.airquality.service.AirQualitySyncService;
import pl.matkmiec.backend.weather.config.CacheConfig;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/air-quality")
@RequiredArgsConstructor
@Tag(name = "Air Quality Admin", description = "Administration endpoints for air quality data synchronization, cleanup, and station management")
public class AirQualityAdminController {

    private final AirQualitySyncService airQualitySyncService;
    private final AirQualityStationManagementService stationManagementService;
    private final AirQualitySyncLogService syncLogService;
    private final AirQualityCleanupService cleanupService;

    @PostMapping("/sync/stations")
    @Operation(summary = "Manually trigger GIOŚ stations synchronization")
    public ResponseEntity<Map<String, Object>> triggerStationsSync() {
        int records = airQualitySyncService.syncStations();
        return ResponseEntity.ok(Map.of("provider", "GIOS_STATIONS", "recordsProcessed", records));
    }

    @PostMapping("/sync/sensors")
    @Operation(summary = "Manually trigger GIOŚ sensors synchronization")
    public ResponseEntity<Map<String, Object>> triggerSensorsSync() {
        int records = airQualitySyncService.syncSensors();
        return ResponseEntity.ok(Map.of("provider", "GIOS_SENSORS", "recordsProcessed", records));
    }

    @PostMapping("/sync/measurements")
    @Operation(summary = "Manually trigger GIOŚ measurement data synchronization")
    public ResponseEntity<Map<String, Object>> triggerMeasurementsSync() {
        int records = airQualitySyncService.syncMeasurements();
        return ResponseEntity.ok(Map.of("provider", "GIOS_MEASUREMENTS", "recordsProcessed", records));
    }

    @PostMapping("/sync/indexes")
    @Operation(summary = "Manually trigger GIOŚ air quality indexes synchronization")
    public ResponseEntity<Map<String, Object>> triggerIndexesSync() {
        int records = airQualitySyncService.syncAirQualityIndexes();
        return ResponseEntity.ok(Map.of("provider", "GIOS_INDEX", "recordsProcessed", records));
    }

    @PostMapping("/sync/forecasts")
    @Operation(summary = "Manually trigger IOŚ-PIB forecasts synchronization")
    public ResponseEntity<Map<String, Object>> triggerForecastsSync() {
        int records = airQualitySyncService.syncForecasts();
        return ResponseEntity.ok(Map.of("provider", "IOS_FORECAST", "recordsProcessed", records));
    }

    @PostMapping("/sync/all")
    @Operation(summary = "Manually trigger complete synchronization pipeline for air quality")
    public ResponseEntity<Map<String, Object>> triggerAllSync() {
        Map<String, Integer> results = airQualitySyncService.syncAll();
        return ResponseEntity.ok(Map.of("status", "SUCCESS", "results", results));
    }

    @PostMapping("/cleanup")
    @Operation(summary = "Manually trigger air quality data cleanup")
    public ResponseEntity<Map<String, Object>> triggerCleanup(
            @RequestParam(required = false) Integer retentionDays
    ) {
        Map<String, Object> result = retentionDays != null
                ? cleanupService.cleanupDataOlderThan(retentionDays)
                : cleanupService.cleanupOldData();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/sync/logs")
    @Operation(summary = "Fetch air quality synchronization logs with pagination")
    public ResponseEntity<Page<AirQualitySyncLog>> getSyncLogs(
            @ParameterObject @PageableDefault(sort = "startedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(syncLogService.getLogs(pageable));
    }

    @GetMapping("/stations")
    @Operation(summary = "Get all GIOŚ stations (active and inactive)")
    public ResponseEntity<List<GiosStation>> getAllStations() {
        return ResponseEntity.ok(stationManagementService.getAllStations());
    }

    @PatchMapping("/stations/{id}/toggle-active")
    @Operation(summary = "Toggle active status for a GIOŚ station")
    @CacheEvict(value = {
            CacheConfig.CACHE_AIR_QUALITY_MAP,
            CacheConfig.CACHE_AIR_QUALITY_STATIONS,
            CacheConfig.CACHE_AIR_QUALITY_RANKINGS,
            CacheConfig.CACHE_AIR_QUALITY_STATION_DETAILS,
            CacheConfig.CACHE_AIR_QUALITY_NEAREST,
            CacheConfig.CACHE_AIR_QUALITY_OVERVIEW
    }, allEntries = true)
    public ResponseEntity<Map<String, Object>> toggleStationActive(@PathVariable Long id) {
        boolean active = stationManagementService.toggleStationActive(id);
        return ResponseEntity.ok(Map.of("stationId", id, "isActive", active));
    }

    @PatchMapping("/stations/{id}/teryt")
    @Operation(summary = "Update TERYT code for a GIOŚ station")
    @CacheEvict(value = {
            CacheConfig.CACHE_AIR_QUALITY_MAP,
            CacheConfig.CACHE_AIR_QUALITY_STATION_DETAILS,
            CacheConfig.CACHE_AIR_QUALITY_FORECAST
    }, allEntries = true)
    public ResponseEntity<GiosStation> updateStationTeryt(
            @PathVariable Long id,
            @RequestParam String terytCode
    ) {
        GiosStation station = stationManagementService.updateStationTeryt(id, terytCode);
        return ResponseEntity.ok(station);
    }
}
