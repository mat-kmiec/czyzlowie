package pl.matkmiec.backend.weather.mapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pl.matkmiec.backend.weather.dto.ImgwHydroResponseDto;
import pl.matkmiec.backend.weather.dto.ImgwMeteoResponseDto;
import pl.matkmiec.backend.weather.dto.ImgwSynopResponseDto;
import pl.matkmiec.backend.weather.model.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.stream.Stream;

@Slf4j
@Component
public class ImgwDataMapper {

    private static final DateTimeFormatter FULL_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ImgwSynopData toSynopDataEntity(ImgwSynopResponseDto dto, ImgwSynopStation station) {
        LocalDateTime measurementTime = parseSynopDateTime(dto.dataPomiaru(), dto.godzinaPomiaru());
        if (measurementTime == null) {
            return null;
        }

        return ImgwSynopData.builder()
                .station(station)
                .measurementTime(measurementTime)
                .temperature(dto.temperatura())
                .windSpeed(dto.predkoscWiatru())
                .windDirection(dto.kierunekWiatru())
                .relativeHumidity(dto.wilgotnoscWzgledna())
                .precipitation(dto.sumaOpadu())
                .pressure(dto.cisnienie())
                .build();
    }

    public ImgwHydroData toHydroDataEntity(ImgwHydroResponseDto dto, ImgwHydroStation station) {
        LocalDateTime measurementTime = resolveHydroMeasurementTime(dto);
        if (measurementTime == null) {
            return null;
        }

        return ImgwHydroData.builder()
                .station(station)
                .measurementTime(measurementTime)
                .waterLevel(dto.stanWody())
                .waterTemperature(dto.temperaturaWody())
                .flow(dto.przeplyw())
                .icePhenomenon(dto.zjawiskoLodowe())
                .overgrowthPhenomenon(dto.zjawiskoZarastania())
                .build();
    }

    public ImgwMeteoData toMeteoDataEntity(ImgwMeteoResponseDto dto, ImgwMeteoStation station) {
        LocalDateTime measurementTime = resolveMeteoMeasurementTime(dto);
        if (measurementTime == null) {
            return null;
        }

        return ImgwMeteoData.builder()
                .station(station)
                .measurementTime(measurementTime)
                .groundTemperature(dto.temperaturaGruntu())
                .airTemperature(dto.temperaturaPowietrza())
                .windSpeed(dto.wiatrSredniaPredkosc())
                .windDirection(dto.wiatrKierunek() != null ? (int) Math.round(dto.wiatrKierunek()) : null)
                .windGust10min(dto.wiatrPoryw10min())
                .relativeHumidity(dto.wilgotnoscWzgledna())
                .precipitation10min(dto.opad10min())
                .build();
    }

    public LocalDateTime parseSynopDateTime(String date, String hour) {
        if (date == null || date.isBlank() || hour == null || hour.isBlank()) {
            return null;
        }
        try {
            LocalDate parsedDate = LocalDate.parse(date.trim());
            int parsedHour = Integer.parseInt(hour.trim());
            return parsedDate.atTime(parsedHour, 0);
        } catch (Exception e) {
            log.debug("Nie udało się sparsować daty synop: date={}, hour={}", date, hour);
            return null;
        }
    }

    public LocalDateTime resolveHydroMeasurementTime(ImgwHydroResponseDto dto) {
        if (dto.stanWodyDataPomiaru() != null && !dto.stanWodyDataPomiaru().isBlank()) {
            LocalDateTime parsed = parseFullDateTime(dto.stanWodyDataPomiaru());
            if (parsed != null) return parsed;
        }
        return Stream.of(
                dto.temperaturaWodyDataPomiaru(),
                dto.przeplywData(),
                dto.zjawiskoZarastaniaDataPomiaru(),
                dto.zjawiskoLodoweDataPomiaru()
        )
                .filter(Objects::nonNull)
                .filter(s -> !s.isBlank())
                .map(this::parseFullDateTime)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
    }

    public LocalDateTime resolveMeteoMeasurementTime(ImgwMeteoResponseDto dto) {
        return Stream.of(
                dto.temperaturaPowietrzaData(),
                dto.opad10minData(),
                dto.temperaturaGruntuData(),
                dto.wiatrSredniaPredkoscData(),
                dto.wilgotnoscWzglednaData(),
                dto.wiatrPredkoscMaksymalnaData(),
                dto.wiatrPoryw10minData(),
                dto.wiatrKierunekData()
        )
                .filter(Objects::nonNull)
                .filter(s -> !s.isBlank())
                .map(this::parseFullDateTime)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
    }

    public LocalDateTime parseFullDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateTimeStr.trim(), FULL_DATE_FORMAT);
        } catch (Exception e) {
            log.debug("Nie udało się sparsować pełnej daty: {}", dateTimeStr);
            return null;
        }
    }
}
