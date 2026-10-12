package pl.matkmiec.backend.airquality.mapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pl.matkmiec.backend.airquality.dto.*;
import pl.matkmiec.backend.airquality.model.GiosAirQualityIndex;
import pl.matkmiec.backend.airquality.model.GiosMeasurement;
import pl.matkmiec.backend.airquality.model.GiosSensor;
import pl.matkmiec.backend.airquality.model.GiosStation;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Slf4j
@Component
public class GiosDataMapper {

    private static final DateTimeFormatter DATE_TIME_SPACE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public GiosStation toStationEntity(GiosStationResponseDto dto) {
        if (dto == null || dto.id() == null) {
            return null;
        }

        String cityName = null;
        Long cityId = null;
        String communeName = null;
        String districtName = null;
        String provinceName = null;

        if (dto.city() != null) {
            cityId = dto.city().id();
            cityName = dto.city().name();
            if (dto.city().commune() != null) {
                communeName = dto.city().commune().communeName();
                districtName = dto.city().commune().districtName();
                provinceName = dto.city().commune().provinceName();
            }
        }

        return GiosStation.builder()
                .id(dto.id())
                .stationName(dto.stationName() != null ? dto.stationName() : "Stacja " + dto.id())
                .gegrLat(dto.gegrLat())
                .gegrLon(dto.gegrLon())
                .addressStreet(dto.addressStreet())
                .cityId(cityId)
                .cityName(cityName)
                .communeName(communeName)
                .districtName(districtName)
                .provinceName(provinceName)
                .isActive(true)
                .lastSyncAt(LocalDateTime.now())
                .build();
    }

    public GiosSensor toSensorEntity(GiosSensorResponseDto dto, GiosStation station) {
        if (dto == null || dto.id() == null) {
            return null;
        }

        String paramName = "";
        String paramFormula = "";
        String paramCode = "";
        Integer idParam = null;

        if (dto.param() != null) {
            paramName = dto.param().paramName() != null ? dto.param().paramName() : "";
            paramFormula = dto.param().paramFormula() != null ? dto.param().paramFormula() : "";
            paramCode = dto.param().paramCode() != null ? dto.param().paramCode() : paramFormula;
            idParam = dto.param().idParam();
        }

        return GiosSensor.builder()
                .id(dto.id())
                .station(station)
                .paramName(paramName)
                .paramFormula(paramFormula)
                .paramCode(paramCode)
                .idParam(idParam)
                .isActive(true)
                .build();
    }

    public GiosMeasurement toMeasurementEntity(GiosMeasurementValueDto valueDto, GiosSensor sensor, String paramCode) {
        if (valueDto == null || valueDto.date() == null || sensor == null) {
            return null;
        }

        LocalDateTime measurementTime = parseDateTime(valueDto.date());
        if (measurementTime == null) {
            return null;
        }

        return GiosMeasurement.builder()
                .sensor(sensor)
                .stationId(sensor.getStation().getId())
                .paramCode(paramCode != null ? paramCode : sensor.getParamCode())
                .measurementTime(measurementTime)
                .value(valueDto.value())
                .build();
    }

    public GiosAirQualityIndex toIndexEntity(GiosIndexResponseDto dto, GiosStation station) {
        if (dto == null || station == null) {
            return null;
        }

        Integer indexLevelId = null;
        String indexLevelName = null;
        if (dto.stIndexLevel() != null) {
            indexLevelId = dto.stIndexLevel().id();
            indexLevelName = dto.stIndexLevel().indexLevelName();
        }

        return GiosAirQualityIndex.builder()
                .station(station)
                .calculationDate(parseDateTime(dto.stCalcDate()))
                .sourceDataDate(parseDateTime(dto.stSourceDataDate()))
                .indexLevelId(indexLevelId)
                .indexLevelName(indexLevelName)
                .stIndexStatus(dto.stIndexStatus())
                .stIndexCrParam(dto.stIndexCrParam())
                .pm10IndexLevel(dto.pm10IndexLevel() != null ? dto.pm10IndexLevel().indexLevelName() : null)
                .pm10SourceDataDate(parseDateTime(dto.pm10SourceDataDate()))
                .pm25IndexLevel(dto.pm25IndexLevel() != null ? dto.pm25IndexLevel().indexLevelName() : null)
                .pm25SourceDataDate(parseDateTime(dto.pm25SourceDataDate()))
                .o3IndexLevel(dto.o3IndexLevel() != null ? dto.o3IndexLevel().indexLevelName() : null)
                .o3SourceDataDate(parseDateTime(dto.o3SourceDataDate()))
                .no2IndexLevel(dto.no2IndexLevel() != null ? dto.no2IndexLevel().indexLevelName() : null)
                .no2SourceDataDate(parseDateTime(dto.no2SourceDataDate()))
                .so2IndexLevel(dto.so2IndexLevel() != null ? dto.so2IndexLevel().indexLevelName() : null)
                .so2SourceDataDate(parseDateTime(dto.so2SourceDataDate()))
                .coIndexLevel(dto.coIndexLevel() != null ? dto.coIndexLevel().indexLevelName() : null)
                .coSourceDataDate(parseDateTime(dto.coSourceDataDate()))
                .c6h6IndexLevel(dto.c6h6IndexLevel() != null ? dto.c6h6IndexLevel().indexLevelName() : null)
                .c6h6SourceDataDate(parseDateTime(dto.c6h6SourceDataDate()))
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public LocalDateTime parseDateTime(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        String clean = dateStr.trim();
        try {
            if (clean.contains(" ")) {
                return LocalDateTime.parse(clean, DATE_TIME_SPACE_FORMATTER);
            } else if (clean.contains("T")) {
                return LocalDateTime.parse(clean, DateTimeFormatter.ISO_DATE_TIME);
            } else {
                return LocalDateTime.parse(clean, DATE_TIME_SPACE_FORMATTER);
            }
        } catch (DateTimeParseException e) {
            log.warn("Nie udało się sparsować daty z GIOŚ '{}': {}", dateStr, e.getMessage());
            return null;
        }
    }
}
