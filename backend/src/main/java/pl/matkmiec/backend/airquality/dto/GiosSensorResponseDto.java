package pl.matkmiec.backend.airquality.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GiosSensorResponseDto(
        @JsonProperty("id") Long id,
        @JsonProperty("stationId") Long stationId,
        @JsonProperty("param") GiosSensorParamDto param
) {}
