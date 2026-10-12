package pl.matkmiec.backend.airquality.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GiosSensorParamDto(
        @JsonProperty("paramName") String paramName,
        @JsonProperty("paramFormula") String paramFormula,
        @JsonProperty("paramCode") String paramCode,
        @JsonProperty("idParam") Integer idParam
) {}
