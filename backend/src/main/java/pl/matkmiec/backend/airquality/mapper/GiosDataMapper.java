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

        String cityName = dto.cityName();
        Long cityId = dto.cityId();
        String communeName = dto.communeName();
        String districtName = dto.districtName();
        String provinceName = dto.provinceName();

        if (dto.city() != null) {
            if (cityId == null) cityId = dto.city().id();
            if (cityName == null) cityName = dto.city().name();
            if (dto.city().commune() != null) {
                if (communeName == null) communeName = dto.city().commune().communeName();
                if (districtName == null) districtName = dto.city().commune().districtName();
                if (provinceName == null) provinceName = dto.city().commune().provinceName();
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

        String paramName = dto.paramName() != null ? dto.paramName() : "";
        String paramFormula = dto.paramFormula() != null ? dto.paramFormula() : "";
        String paramCode = dto.paramCode() != null ? dto.paramCode() : "";
        Integer idParam = dto.idParam();

        if (dto.param() != null) {
            if (paramName.isEmpty() && dto.param().paramName() != null) paramName = dto.param().paramName();
            if (paramFormula.isEmpty() && dto.param().paramFormula() != null) paramFormula = dto.param().paramFormula();
            if (paramCode.isEmpty() && dto.param().paramCode() != null) paramCode = dto.param().paramCode();
            if (idParam == null) idParam = dto.param().idParam();
        }

        if (paramCode.isEmpty()) {
            paramCode = !paramFormula.isEmpty() ? paramFormula : paramName;
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

        Integer indexLevelId = dto.stIndexLevelId();
        String indexLevelName = dto.stIndexLevelName();
        if (dto.stIndexLevel() != null) {
            if (indexLevelId == null) indexLevelId = dto.stIndexLevel().id();
            if (indexLevelName == null) indexLevelName = dto.stIndexLevel().indexLevelName();
        }

        String pm10Level = dto.pm10IndexLevelName();
        if (pm10Level == null && dto.pm10IndexLevel() != null) pm10Level = dto.pm10IndexLevel().indexLevelName();

        String pm25Level = dto.pm25IndexLevelName();
        if (pm25Level == null && dto.pm25IndexLevel() != null) pm25Level = dto.pm25IndexLevel().indexLevelName();

        String o3Level = dto.o3IndexLevelName();
        if (o3Level == null && dto.o3IndexLevel() != null) o3Level = dto.o3IndexLevel().indexLevelName();

        String no2Level = dto.no2IndexLevelName();
        if (no2Level == null && dto.no2IndexLevel() != null) no2Level = dto.no2IndexLevel().indexLevelName();

        String so2Level = dto.so2IndexLevelName();
        if (so2Level == null && dto.so2IndexLevel() != null) so2Level = dto.so2IndexLevel().indexLevelName();

        String coLevel = dto.coIndexLevelName();
        if (coLevel == null && dto.coIndexLevel() != null) coLevel = dto.coIndexLevel().indexLevelName();

        String c6h6Level = dto.c6h6IndexLevelName();
        if (c6h6Level == null && dto.c6h6IndexLevel() != null) c6h6Level = dto.c6h6IndexLevel().indexLevelName();

        return GiosAirQualityIndex.builder()
                .station(station)
                .calculationDate(parseDateTime(dto.stCalcDate()))
                .sourceDataDate(parseDateTime(dto.stSourceDataDate()))
                .indexLevelId(indexLevelId)
                .indexLevelName(indexLevelName)
                .stIndexStatus(dto.stIndexStatus())
                .stIndexCrParam(dto.stIndexCrParam())
                .pm10IndexLevel(pm10Level)
                .pm10SourceDataDate(parseDateTime(dto.pm10SourceDataDate()))
                .pm25IndexLevel(pm25Level)
                .pm25SourceDataDate(parseDateTime(dto.pm25SourceDataDate()))
                .o3IndexLevel(o3Level)
                .o3SourceDataDate(parseDateTime(dto.o3SourceDataDate()))
                .no2IndexLevel(no2Level)
                .no2SourceDataDate(parseDateTime(dto.no2SourceDataDate()))
                .so2IndexLevel(so2Level)
                .so2SourceDataDate(parseDateTime(dto.so2SourceDataDate()))
                .coIndexLevel(coLevel)
                .coSourceDataDate(parseDateTime(dto.coSourceDataDate()))
                .c6h6IndexLevel(c6h6Level)
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
