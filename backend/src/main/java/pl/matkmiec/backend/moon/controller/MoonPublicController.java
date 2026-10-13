package pl.matkmiec.backend.moon.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.matkmiec.backend.moon.dto.MoonDailyDto;
import pl.matkmiec.backend.moon.dto.MoonMonthlyDto;
import pl.matkmiec.backend.moon.dto.MoonPhaseDto;
import pl.matkmiec.backend.moon.dto.SolunarDayDto;
import pl.matkmiec.backend.moon.service.MoonService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/v1/moon")
@RequiredArgsConstructor
@Tag(name = "Moon & Solunar Public", description = "Public endpoints for astronomical data, Moon phases, and Solunar fishing forecasts")
public class MoonPublicController {

    private final MoonService moonService;

    @GetMapping("/daily")
    @Operation(summary = "Get comprehensive astronomical and solunar data for a specific day",
            description = "Returns Moon phase, illumination percentage, age, perigee/apogee, Sun and Moon rise/set/zenith times, golden hours, and fish feeding periods.")
    public ResponseEntity<MoonDailyDto> getDailyMoonData(
            @Parameter(description = "Latitude (e.g. 52.2297)") @RequestParam(required = false) Double lat,
            @Parameter(description = "Longitude (e.g. 21.0122)") @RequestParam(required = false) Double lon,
            @Parameter(description = "Meteo, Synop, or GIOS station ID") @RequestParam(required = false) String stationId,
            @Parameter(description = "Query date (YYYY-MM-DD)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(moonService.getDailyData(lat, lon, stationId, date));
    }

    @GetMapping("/monthly")
    @Operation(summary = "Get astronomical and fishing calendar for an entire month",
            description = "Returns daily overview for each day of the month (phases, illumination, sun/moon times, solunar ratings, icon codes) for frontend calendar views.")
    public ResponseEntity<MoonMonthlyDto> getMonthlyMoonData(
            @Parameter(description = "Latitude") @RequestParam(required = false) Double lat,
            @Parameter(description = "Longitude") @RequestParam(required = false) Double lon,
            @Parameter(description = "Station ID") @RequestParam(required = false) String stationId,
            @Parameter(description = "Year (e.g. 2026)") @RequestParam(required = false) Integer year,
            @Parameter(description = "Month (1-12)") @RequestParam(required = false) Integer month) {
        return ResponseEntity.ok(moonService.getMonthlyData(lat, lon, stationId, year, month));
    }

    @GetMapping("/solunar/daily")
    @Operation(summary = "Get dedicated Solunar fishing forecast summary",
            description = "Returns Major and Minor feeding periods, overlaps with golden hours, and solunar rating score (1-5 stars).")
    public ResponseEntity<SolunarDayDto> getSolunarSummary(
            @Parameter(description = "Latitude") @RequestParam(required = false) Double lat,
            @Parameter(description = "Longitude") @RequestParam(required = false) Double lon,
            @Parameter(description = "Station ID") @RequestParam(required = false) String stationId,
            @Parameter(description = "Query date") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(moonService.getSolunarSummary(lat, lon, stationId, date));
    }

    @GetMapping("/station/{stationId}")
    @Operation(summary = "Get astronomical and fishing data by IMGW or GIOS station ID")
    public ResponseEntity<MoonDailyDto> getDailyDataForStation(
            @Parameter(description = "Synop, Meteo, or GIOS station ID") @PathVariable String stationId,
            @Parameter(description = "Query date") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(moonService.getDailyData(null, null, stationId, date));
    }

    @GetMapping("/phases")
    @Operation(summary = "Get list of Moon phases with names and icon codes for frontend")
    public ResponseEntity<List<MoonPhaseDto>> getAllMoonPhases() {
        return ResponseEntity.ok(moonService.getAllMoonPhases());
    }
}
