package pl.matkmiec.backend.weather.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.matkmiec.backend.weather.model.ImgwHydroStation;
import pl.matkmiec.backend.weather.model.ImgwMeteoStation;
import pl.matkmiec.backend.weather.model.ImgwSynopStation;
import pl.matkmiec.backend.weather.repository.ImgwHydroStationRepository;
import pl.matkmiec.backend.weather.repository.ImgwMeteoStationRepository;
import pl.matkmiec.backend.weather.repository.ImgwSynopStationRepository;

import java.util.List;

@RestController
@RequestMapping("/public/weather")
@RequiredArgsConstructor
@Tag(name = "Weather Public", description = "Public weather data and active stations")
public class WeatherPublicController {

    private final ImgwSynopStationRepository synopStationRepo;
    private final ImgwMeteoStationRepository meteoStationRepo;
    private final ImgwHydroStationRepository hydroStationRepo;

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
}
