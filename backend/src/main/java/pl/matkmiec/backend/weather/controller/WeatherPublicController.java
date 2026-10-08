package pl.matkmiec.backend.weather.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.matkmiec.backend.weather.model.ImgwHydroStation;
import pl.matkmiec.backend.weather.model.ImgwMeteoStation;
import pl.matkmiec.backend.weather.model.ImgwSynopStation;
import pl.matkmiec.backend.weather.model.OpenMeteoForecast;
import pl.matkmiec.backend.weather.repository.ImgwHydroStationRepository;
import pl.matkmiec.backend.weather.repository.ImgwMeteoStationRepository;
import pl.matkmiec.backend.weather.repository.ImgwSynopStationRepository;
import pl.matkmiec.backend.weather.repository.OpenMeteoForecastRepository;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/public/weather")
@RequiredArgsConstructor
@Tag(name = "Weather Public", description = "Public weather data, active stations and forecasts")
public class WeatherPublicController {

    private final ImgwSynopStationRepository synopStationRepo;
    private final ImgwMeteoStationRepository meteoStationRepo;
    private final ImgwHydroStationRepository hydroStationRepo;
    private final OpenMeteoForecastRepository openMeteoForecastRepo;

    @GetMapping("/stations/synop")
    @Operation(summary = "Get all active Synop stations")
    public ResponseEntity<List<ImgwSynopStation>> getActiveSynopStations() {
        return ResponseEntity.ok(synopStationRepo.findAllByIsActiveTrue());
    }

    @GetMapping("/stations/meteo")
    @Operation(summary = "Get all active Meteo stations")
    public ResponseEntity<List<ImgwMeteoStation>> getActiveMeteoStations() {
        return ResponseEntity.ok(meteoStationRepo.findAllByIsActiveTrue());
    }

    @GetMapping("/stations/hydro")
    @Operation(summary = "Get all active Hydro stations")
    public ResponseEntity<List<ImgwHydroStation>> getActiveHydroStations() {
        return ResponseEntity.ok(hydroStationRepo.findAllByIsActiveTrue());
    }

    @GetMapping("/forecast/{stationId}")
    @Operation(summary = "Get weather forecast for a specific Synop station")
    public ResponseEntity<List<OpenMeteoForecast>> getForecastForStation(
            @PathVariable String stationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from
    ) {
        LocalDateTime startTime = from != null ? from : LocalDateTime.now().minusHours(1);
        return ResponseEntity.ok(openMeteoForecastRepo.findAllBySynopStation_IdAndForecastTimeAfterOrderByForecastTimeAsc(stationId, startTime));
    }
}
