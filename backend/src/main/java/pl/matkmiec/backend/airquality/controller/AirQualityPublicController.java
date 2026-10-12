package pl.matkmiec.backend.airquality.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.matkmiec.backend.airquality.dto.*;
import pl.matkmiec.backend.airquality.model.GiosMeasurement;
import pl.matkmiec.backend.airquality.model.GiosStation;
import pl.matkmiec.backend.airquality.service.AirQualityQueryService;
import pl.matkmiec.backend.airquality.service.AirQualityStationManagementService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/public/air-quality")
@RequiredArgsConstructor
@Tag(name = "Air Quality Public", description = "Public air quality data, stations, map points, rankings, forecasts, and geolocation")
public class AirQualityPublicController {

    private final AirQualityQueryService airQualityQueryService;
    private final AirQualityStationManagementService stationManagementService;

    @GetMapping("/map")
    @Operation(summary = "Get air quality data points for interactive map")
    public ResponseEntity<List<AirQualityMapPointDto>> getMapPoints() {
        return ResponseEntity.ok(airQualityQueryService.getMapPoints());
    }

    @GetMapping("/rankings")
    @Operation(summary = "Get air quality rankings for cleanest or most polluted stations")
    public ResponseEntity<List<AirQualityRankingDto>> getRankings(
            @RequestParam(required = false, defaultValue = "PM10") String pollutant,
            @RequestParam(required = false, defaultValue = "BEST") String order,
            @RequestParam(required = false, defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(airQualityQueryService.getRankings(pollutant, order, limit));
    }

    @GetMapping("/stations")
    @Operation(summary = "Get list of all active air quality stations")
    public ResponseEntity<List<GiosStation>> getActiveStations() {
        return ResponseEntity.ok(stationManagementService.getActiveStations());
    }

    @GetMapping("/stations/{id}")
    @Operation(summary = "Get detailed air quality station information, latest measurements, 24h history and forecasts")
    public ResponseEntity<AirQualityStationDetailsDto> getStationDetails(@PathVariable Long id) {
        return ResponseEntity.ok(airQualityQueryService.getStationDetails(id));
    }

    @GetMapping("/stations/{id}/forecast")
    @Operation(summary = "Get air quality forecast for a specific station based on its TERYT code")
    public ResponseEntity<List<AirQualityForecastDto>> getStationForecast(@PathVariable Long id) {
        return ResponseEntity.ok(airQualityQueryService.getForecastForStation(id));
    }

    @GetMapping("/nearest")
    @Operation(summary = "Find nearest active air quality station and its current readings by GPS coordinates")
    public ResponseEntity<NearestAirQualityStationDto> getNearestStation(
            @RequestParam Double lat,
            @RequestParam Double lon
    ) {
        return ResponseEntity.ok(airQualityQueryService.findNearestStation(lat, lon));
    }

    @GetMapping("/forecast")
    @Operation(summary = "Get air quality forecasts by TERYT code and optional pollutant (PM10, NO2, SO2, O3)")
    public ResponseEntity<List<AirQualityForecastDto>> getForecast(
            @RequestParam String teryt,
            @RequestParam(required = false) String pollutant
    ) {
        return ResponseEntity.ok(airQualityQueryService.getForecast(teryt, pollutant));
    }

    @GetMapping("/history")
    @Operation(summary = "Get paginated historical measurement data for a station and pollutant")
    public ResponseEntity<Page<GiosMeasurement>> getHistory(
            @RequestParam Long stationId,
            @RequestParam(required = false) String pollutant,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @ParameterObject @PageableDefault(sort = "measurementTime", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(airQualityQueryService.getMeasurementHistory(stationId, pollutant, from, to, pageable));
    }

    @GetMapping("/overview")
    @Operation(summary = "Get national air quality summary, index levels distribution and top stations")
    public ResponseEntity<AirQualityOverviewDto> getOverview() {
        return ResponseEntity.ok(airQualityQueryService.getOverview());
    }
}
