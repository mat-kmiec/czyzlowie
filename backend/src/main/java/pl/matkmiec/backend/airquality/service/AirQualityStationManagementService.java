package pl.matkmiec.backend.airquality.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.matkmiec.backend.airquality.dto.GiosSensorResponseDto;
import pl.matkmiec.backend.airquality.dto.GiosStationResponseDto;
import pl.matkmiec.backend.airquality.mapper.GiosDataMapper;
import pl.matkmiec.backend.airquality.model.GiosSensor;
import pl.matkmiec.backend.airquality.model.GiosStation;
import pl.matkmiec.backend.airquality.repository.GiosSensorRepository;
import pl.matkmiec.backend.airquality.repository.GiosStationRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AirQualityStationManagementService {

    private final GiosStationRepository stationRepo;
    private final GiosSensorRepository sensorRepo;
    private final GiosDataMapper mapper;

    @Transactional
    public Map<Long, GiosStation> autoDiscoverStations(List<GiosStationResponseDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, GiosStation> existingStations = stationRepo.findAll().stream()
                .collect(Collectors.toMap(GiosStation::getId, Function.identity(), (a, b) -> a));

        List<GiosStation> toSave = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (GiosStationResponseDto dto : dtos) {
            if (dto.id() == null) continue;

            GiosStation station = existingStations.get(dto.id());
            if (station == null) {
                GiosStation newStation = mapper.toStationEntity(dto);
                if (newStation != null) {
                    // Try to infer TERYT code if city id or commune is available
                    if (dto.city() != null && dto.city().id() != null) {
                        newStation.setTerytCode(String.valueOf(dto.city().id()));
                    }
                    newStation.setLastSyncAt(now);
                    toSave.add(newStation);
                    existingStations.put(newStation.getId(), newStation);
                }
            } else {
                boolean changed = false;
                if (dto.stationName() != null && !dto.stationName().equals(station.getStationName())) {
                    station.setStationName(dto.stationName());
                    changed = true;
                }
                if (dto.gegrLat() != null && !dto.gegrLat().equals(station.getGegrLat())) {
                    station.setGegrLat(dto.gegrLat());
                    changed = true;
                }
                if (dto.gegrLon() != null && !dto.gegrLon().equals(station.getGegrLon())) {
                    station.setGegrLon(dto.gegrLon());
                    changed = true;
                }
                if (dto.addressStreet() != null && !dto.addressStreet().equals(station.getAddressStreet())) {
                    station.setAddressStreet(dto.addressStreet());
                    changed = true;
                }
                if (dto.city() != null) {
                    if (dto.city().name() != null && !dto.city().name().equals(station.getCityName())) {
                        station.setCityName(dto.city().name());
                        changed = true;
                    }
                    if (dto.city().id() != null && !dto.city().id().equals(station.getCityId())) {
                        station.setCityId(dto.city().id());
                        changed = true;
                    }
                    if (dto.city().commune() != null) {
                        if (dto.city().commune().communeName() != null && !dto.city().commune().communeName().equals(station.getCommuneName())) {
                            station.setCommuneName(dto.city().commune().communeName());
                            changed = true;
                        }
                        if (dto.city().commune().districtName() != null && !dto.city().commune().districtName().equals(station.getDistrictName())) {
                            station.setDistrictName(dto.city().commune().districtName());
                            changed = true;
                        }
                        if (dto.city().commune().provinceName() != null && !dto.city().commune().provinceName().equals(station.getProvinceName())) {
                            station.setProvinceName(dto.city().commune().provinceName());
                            changed = true;
                        }
                    }
                }
                station.setLastSyncAt(now);
                if (changed) {
                    toSave.add(station);
                }
            }
        }

        if (!toSave.isEmpty()) {
            stationRepo.saveAll(toSave);
            log.info("Zaktualizowano/dodano {} stacji GIOŚ.", toSave.size());
        }

        return existingStations;
    }

    @Transactional
    public List<GiosSensor> autoDiscoverSensors(GiosStation station, List<GiosSensorResponseDto> dtos) {
        if (station == null || dtos == null || dtos.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, GiosSensor> existingSensors = sensorRepo.findAllByStationId(station.getId()).stream()
                .collect(Collectors.toMap(GiosSensor::getId, Function.identity(), (a, b) -> a));

        List<GiosSensor> toSave = new ArrayList<>();
        List<GiosSensor> allSensors = new ArrayList<>();

        for (GiosSensorResponseDto dto : dtos) {
            if (dto.id() == null) continue;

            GiosSensor sensor = existingSensors.get(dto.id());
            if (sensor == null) {
                GiosSensor newSensor = mapper.toSensorEntity(dto, station);
                if (newSensor != null) {
                    toSave.add(newSensor);
                    allSensors.add(newSensor);
                }
            } else {
                boolean changed = false;
                if (dto.param() != null) {
                    if (dto.param().paramCode() != null && !dto.param().paramCode().equals(sensor.getParamCode())) {
                        sensor.setParamCode(dto.param().paramCode());
                        changed = true;
                    }
                    if (dto.param().paramFormula() != null && !dto.param().paramFormula().equals(sensor.getParamFormula())) {
                        sensor.setParamFormula(dto.param().paramFormula());
                        changed = true;
                    }
                    if (dto.param().paramName() != null && !dto.param().paramName().equals(sensor.getParamName())) {
                        sensor.setParamName(dto.param().paramName());
                        changed = true;
                    }
                    if (dto.param().idParam() != null && !dto.param().idParam().equals(sensor.getIdParam())) {
                        sensor.setIdParam(dto.param().idParam());
                        changed = true;
                    }
                }
                if (changed) {
                    toSave.add(sensor);
                }
                allSensors.add(sensor);
            }
        }

        if (!toSave.isEmpty()) {
            sensorRepo.saveAll(toSave);
        }

        return allSensors;
    }

    @Transactional(readOnly = true)
    public List<GiosStation> getAllStations() {
        return stationRepo.findAll();
    }

    @Transactional(readOnly = true)
    public List<GiosStation> getActiveStations() {
        return stationRepo.findAllByIsActiveTrue();
    }

    @Transactional
    public boolean toggleStationActive(Long id) {
        GiosStation station = stationRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Stacja GIOŚ o ID " + id + " nie istnieje."));
        boolean newState = !Boolean.TRUE.equals(station.getIsActive());
        station.setIsActive(newState);
        stationRepo.save(station);
        log.info("Zmieniono status aktywności stacji GIOŚ {} na: {}", id, newState);
        return newState;
    }

    @Transactional
    public GiosStation updateStationTeryt(Long id, String terytCode) {
        GiosStation station = stationRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Stacja GIOŚ o ID " + id + " nie istnieje."));
        station.setTerytCode(terytCode != null ? terytCode.trim() : null);
        return stationRepo.save(station);
    }
}
