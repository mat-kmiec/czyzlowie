package pl.matkmiec.backend.moon.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.matkmiec.backend.moon.dto.MoonGenerationResultDto;
import pl.matkmiec.backend.moon.dto.MoonYearlyAdminDto;
import pl.matkmiec.backend.moon.service.MoonService;

@RestController
@RequestMapping("/v1/admin/moon")
@RequiredArgsConstructor
@Tag(name = "Moon Admin", description = "Administration endpoints for bulk astronomical and solunar data calculations and persistence")
public class MoonAdminController {

    private final MoonService moonService;

    @PostMapping("/generate-year")
    @Operation(summary = "Calculate and persist full astronomical and solunar data for an entire year",
            description = "Fills database with precomputed data: first nationwide general records (phases, age, distance), then per-synop station precise sun/moon times and solunar feeding periods.")
    public ResponseEntity<MoonGenerationResultDto> generateYearlyMoonData(
            @Parameter(description = "Calculation year (e.g. 2027)") @RequestParam(defaultValue = "2027") int year) {
        return ResponseEntity.ok(moonService.generateYearlyData(year));
    }

    @GetMapping("/year")
    @Operation(summary = "Get yearly summary of astronomical and solunar data",
            description = "Fetches precomputed yearly data from database or computes dynamically for yearly analytics, moon phase calendars, and fishing charts.")
    public ResponseEntity<MoonYearlyAdminDto> getYearlyMoonData(
            @Parameter(description = "Calculation year (e.g. 2027)") @RequestParam(defaultValue = "2027") int year,
            @Parameter(description = "Latitude") @RequestParam(required = false) Double lat,
            @Parameter(description = "Longitude") @RequestParam(required = false) Double lon,
            @Parameter(description = "Station ID") @RequestParam(required = false) String stationId) {
        return ResponseEntity.ok(moonService.getYearlyAdminData(year, lat, lon, stationId));
    }
}
