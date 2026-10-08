package pl.matkmiec.backend.weather.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.matkmiec.backend.weather.dto.ImgwHydroResponseDto;
import pl.matkmiec.backend.weather.dto.ImgwMeteoResponseDto;
import pl.matkmiec.backend.weather.dto.ImgwSynopResponseDto;
import pl.matkmiec.backend.weather.model.ImgwHydroStation;
import pl.matkmiec.backend.weather.model.ImgwMeteoStation;
import pl.matkmiec.backend.weather.model.ImgwSynopStation;
import pl.matkmiec.backend.weather.repository.ImgwHydroStationRepository;
import pl.matkmiec.backend.weather.repository.ImgwMeteoStationRepository;
import pl.matkmiec.backend.weather.repository.ImgwSynopStationRepository;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StationManagementService {

    private final ImgwSynopStationRepository synopStationRepo;
    private final ImgwMeteoStationRepository meteoStationRepo;
    private final ImgwHydroStationRepository hydroStationRepo;

    @Transactional
    public Map<String, ImgwSynopStation> autoDiscoverSynopStations(List<ImgwSynopResponseDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, ImgwSynopStation> existingMap = synopStationRepo.findAll().stream()
                .collect(Collectors.toMap(ImgwSynopStation::getId, Function.identity(), (a, b) -> a, HashMap::new));

        List<ImgwSynopStation> newStations = new ArrayList<>();
        for (ImgwSynopResponseDto dto : dtos) {
            if (dto.idStacji() != null && !dto.idStacji().isBlank() && !existingMap.containsKey(dto.idStacji().trim())) {
                String id = dto.idStacji().trim();
                ImgwSynopStation station = ImgwSynopStation.builder()
                        .id(id)
                        .name(dto.stacja() != null && !dto.stacja().isBlank() ? dto.stacja().trim() : id)
                        .isActive(true)
                        .build();
                newStations.add(station);
                existingMap.put(id, station);
            }
        }

        if (!newStations.isEmpty()) {
            synopStationRepo.saveAll(newStations);
            log.info("Wykryto i zapisano {} nowych stacji Synop (auto-discovery).", newStations.size());
        }

        return existingMap;
    }

    @Transactional
    public Map<String, ImgwMeteoStation> autoDiscoverMeteoStations(List<ImgwMeteoResponseDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, ImgwMeteoStation> existingMap = meteoStationRepo.findAll().stream()
                .collect(Collectors.toMap(ImgwMeteoStation::getId, Function.identity(), (a, b) -> a, HashMap::new));

        List<ImgwMeteoStation> newStations = new ArrayList<>();
        for (ImgwMeteoResponseDto dto : dtos) {
            if (dto.kodStacji() != null && !dto.kodStacji().isBlank() && !existingMap.containsKey(dto.kodStacji().trim())) {
                String id = dto.kodStacji().trim();
                ImgwMeteoStation station = ImgwMeteoStation.builder()
                        .id(id)
                        .name(dto.nazwaStacji() != null && !dto.nazwaStacji().isBlank() ? dto.nazwaStacji().trim() : id)
                        .lat(dto.lat())
                        .lon(dto.lon())
                        .elevationAsl(dto.wysokoscNpm())
                        .establishedYear(dto.rokZalozeniaStacji())
                        .isActive(true)
                        .build();
                newStations.add(station);
                existingMap.put(id, station);
            }
        }

        if (!newStations.isEmpty()) {
            meteoStationRepo.saveAll(newStations);
            log.info("Wykryto i zapisano {} nowych stacji Meteo (auto-discovery).", newStations.size());
        }

        return existingMap;
    }

    @Transactional
    public Map<String, ImgwHydroStation> autoDiscoverHydroStations(List<ImgwHydroResponseDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, ImgwHydroStation> existingMap = hydroStationRepo.findAll().stream()
                .collect(Collectors.toMap(ImgwHydroStation::getId, Function.identity(), (a, b) -> a, HashMap::new));

        List<ImgwHydroStation> newStations = new ArrayList<>();
        for (ImgwHydroResponseDto dto : dtos) {
            if (dto.idStacji() != null && !dto.idStacji().isBlank() && !existingMap.containsKey(dto.idStacji().trim())) {
                String id = dto.idStacji().trim();
                ImgwHydroStation station = ImgwHydroStation.builder()
                        .id(id)
                        .name(dto.stacja() != null && !dto.stacja().isBlank() ? dto.stacja().trim() : id)
                        .river(dto.rzeka() != null && !dto.rzeka().isBlank() ? dto.rzeka().trim() : null)
                        .province(dto.wojewodztwo() != null && !dto.wojewodztwo().isBlank() ? dto.wojewodztwo().trim() : null)
                        .lat(dto.lat())
                        .lon(dto.lon())
                        .isActive(true)
                        .build();
                newStations.add(station);
                existingMap.put(id, station);
            }
        }

        if (!newStations.isEmpty()) {
            hydroStationRepo.saveAll(newStations);
            log.info("Wykryto i zapisano {} nowych stacji Hydro (auto-discovery).", newStations.size());
        }

        return existingMap;
    }

    @Transactional(readOnly = true)
    public List<ImgwSynopStation> getAllSynopStations() {
        return synopStationRepo.findAll();
    }

    @Transactional(readOnly = true)
    public List<ImgwMeteoStation> getAllMeteoStations() {
        return meteoStationRepo.findAll();
    }

    @Transactional(readOnly = true)
    public List<ImgwHydroStation> getAllHydroStations() {
        return hydroStationRepo.findAll();
    }

    @Transactional
    public boolean toggleSynopStationActive(String stationId) {
        return synopStationRepo.findById(stationId).map(station -> {
            station.setIsActive(!Boolean.TRUE.equals(station.getIsActive()));
            synopStationRepo.save(station);
            return station.getIsActive();
        }).orElseThrow(() -> new NoSuchElementException("Nie znaleziono stacji synop o id: " + stationId));
    }

    @Transactional
    public boolean toggleMeteoStationActive(String stationId) {
        return meteoStationRepo.findById(stationId).map(station -> {
            station.setIsActive(!Boolean.TRUE.equals(station.getIsActive()));
            meteoStationRepo.save(station);
            return station.getIsActive();
        }).orElseThrow(() -> new NoSuchElementException("Nie znaleziono stacji meteo o id: " + stationId));
    }

    @Transactional
    public boolean toggleHydroStationActive(String stationId) {
        return hydroStationRepo.findById(stationId).map(station -> {
            station.setIsActive(!Boolean.TRUE.equals(station.getIsActive()));
            hydroStationRepo.save(station);
            return station.getIsActive();
        }).orElseThrow(() -> new NoSuchElementException("Nie znaleziono stacji hydro o id: " + stationId));
    }
}
