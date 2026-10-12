package pl.matkmiec.backend.airquality.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GiosMeasurementValueDto(
        @JsonProperty("date") String date,
        @JsonProperty("value") Double value
) {}
