package pl.matkmiec.backend.airquality.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GiosSensorResponseDto(
        @JsonProperty("id")
        @JsonAlias({"Identyfikator stanowiska", "id"})
        Long id,

        @JsonProperty("stationId")
        @JsonAlias({"Identyfikator stacji", "stationId"})
        Long stationId,

        @JsonProperty("param")
        @JsonAlias({"param"})
        GiosSensorParamDto param,

        @JsonProperty("paramName")
        @JsonAlias({"Wskaźnik", "paramName"})
        String paramName,

        @JsonProperty("paramFormula")
        @JsonAlias({"Wskaźnik - wzór", "paramFormula"})
        String paramFormula,

        @JsonProperty("paramCode")
        @JsonAlias({"Wskaźnik - kod", "paramCode"})
        String paramCode,

        @JsonProperty("idParam")
        @JsonAlias({"Id wskaźnika", "idParam"})
        Integer idParam
) {
    public GiosSensorResponseDto(Long id, Long stationId, GiosSensorParamDto param) {
        this(id, stationId, param, null, null, null, null);
    }
}
