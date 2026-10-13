package pl.matkmiec.backend.airquality.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GiosMeasurementDataResponseDto(
        @JsonProperty("key")
        @JsonAlias({"Kod stanowiska", "key"})
        String key,

        @JsonProperty("values")
        @JsonAlias({"Lista danych pomiarowych", "values"})
        List<GiosMeasurementValueDto> values
) {}
