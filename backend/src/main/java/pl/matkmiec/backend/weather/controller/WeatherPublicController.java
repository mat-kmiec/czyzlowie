package pl.matkmiec.backend.weather.controller;

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
import pl.matkmiec.backend.weather.dto.CurrentWeatherDto;
import pl.matkmiec.backend.weather.dto.NearestStationDto;
import pl.matkmiec.backend.weather.dto.NearestStationsOverviewDto;
import pl.matkmiec.backend.weather.model.*;
import pl.matkmiec.backend.weather.service.WeatherQueryService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/public/weather")
@RequiredArgsConstructor
@Tag(name = "Weather Public", description = "Public weather data, active stations, geolocation and forecasts")
public class WeatherPublicController {

    private final WeatherQueryService weatherQueryService;

    @GetMapping("/stations/synop")
    @Operation(summary = "Get all active Synop stations")
    public ResponseEntity<List<ImgwSynopStation>> getActiveSynopStations() {
        return ResponseEntity.ok(weatherQueryService.getActiveSynopStations());
    }

    @GetMapping("/stations/meteo")
    @Operation(summary = "Get all active Meteo stations")
    public ResponseEntity<List<ImgwMeteoStation>> getActiveMeteoStations() {
        return ResponseEntity.ok(weatherQueryService.getActiveMeteoStations());
    }

    @GetMapping("/stations/hydro")
    @Operation(summary = "Get all active Hydro stations")
    public ResponseEntity<List<ImgwHydroStation>> getActiveHydroStations() {
        return ResponseEntity.ok(weatherQueryService.getActiveHydroStations());
    }

    @GetMapping("/forecast/{stationId}")
    @Operation(summary = "Get weather forecast for a specific Synop station")
    public ResponseEntity<List<OpenMeteoForecast>> getForecastForStation(
            @PathVariable String stationId
    ) {
        return ResponseEntity.ok(weatherQueryService.getForecastForStation(stationId));
    }

    @GetMapping("/nearest")
    @Operation(summary = "Find the nearest active weather station by GPS coordinates")
    public ResponseEntity<NearestStationDto> getNearestStation(
            @RequestParam Double lat,
            @RequestParam Double lon,
            @RequestParam(required = false, defaultValue = "ALL") String type
    ) {
        return ResponseEntity.ok(weatherQueryService.findNearestStation(lat, lon, type));
    }

    @GetMapping("/nearest/overview")
    @Operation(summary = "Find the nearest Synop, Meteo and Hydro stations by GPS coordinates")
    public ResponseEntity<NearestStationsOverviewDto> getNearestStationsOverview(
            @RequestParam Double lat,
            @RequestParam Double lon
    ) {
        return ResponseEntity.ok(weatherQueryService.findNearestStationsOverview(lat, lon));
    }

    @GetMapping("/current")
    @Operation(summary = "Get current weather by station ID or GPS coordinates")
    public ResponseEntity<CurrentWeatherDto> getCurrentWeather(
            @RequestParam(required = false) String stationId,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon
    ) {
        if (stationId != null && !stationId.isBlank()) {
            return ResponseEntity.ok(weatherQueryService.getCurrentWeatherByStationId(stationId.trim()));
        } else if (lat != null && lon != null) {
            return ResponseEntity.ok(weatherQueryService.getCurrentWeatherByCoordinates(lat, lon));
        } else {
            throw new IllegalArgumentException("Należy podać parametr 'stationId' lub parę współrzędnych 'lat' i 'lon'.");
        }
    }

    @GetMapping("/current/{stationId}")
    @Operation(summary = "Get current weather for a specific Synop station ID")
    public ResponseEntity<CurrentWeatherDto> getCurrentWeatherByStationId(
            @PathVariable String stationId
    ) {
        return ResponseEntity.ok(weatherQueryService.getCurrentWeatherByStationId(stationId));
    }

    @GetMapping("/history/synop")
    @Operation(summary = "Get paginated historical Synop measurements")
    public ResponseEntity<Page<ImgwSynopData>> getSynopHistory(
            @RequestParam(required = false) String stationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @ParameterObject @PageableDefault(sort = "measurementTime", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(weatherQueryService.getSynopHistory(stationId, from, to, pageable));
    }

    @GetMapping("/history/meteo")
    @Operation(summary = "Get paginated historical Meteo measurements")
    public ResponseEntity<Page<ImgwMeteoData>> getMeteoHistory(
            @RequestParam(required = false) String stationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @ParameterObject @PageableDefault(sort = "measurementTime", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(weatherQueryService.getMeteoHistory(stationId, from, to, pageable));
    }

    @GetMapping("/history/hydro")
    @Operation(summary = "Get paginated historical Hydro measurements")
    public ResponseEntity<Page<ImgwHydroData>> getHydroHistory(
            @RequestParam(required = false) String stationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @ParameterObject @PageableDefault(sort = "measurementTime", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(weatherQueryService.getHydroHistory(stationId, from, to, pageable));
    }
}
