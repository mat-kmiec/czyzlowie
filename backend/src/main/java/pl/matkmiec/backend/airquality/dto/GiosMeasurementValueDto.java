package pl.matkmiec.backend.airquality.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GiosMeasurementValueDto(
        @JsonProperty("date")
        @JsonAlias({"Data", "date"})
        String date,

        @JsonProperty("value")
        @JsonAlias({"Wartość", "value"})
        Double value
) {}
