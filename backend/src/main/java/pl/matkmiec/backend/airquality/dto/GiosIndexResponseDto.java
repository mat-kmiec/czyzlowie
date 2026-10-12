package pl.matkmiec.backend.airquality.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GiosIndexResponseDto(
        @JsonProperty("id") Long id,
        @JsonProperty("stCalcDate") String stCalcDate,
        @JsonProperty("stIndexLevel") GiosIndexLevelDto stIndexLevel,
        @JsonProperty("stSourceDataDate") String stSourceDataDate,
        @JsonProperty("stIndexStatus") Boolean stIndexStatus,
        @JsonProperty("stIndexCrParam") String stIndexCrParam,

        @JsonProperty("pm10CalcDate") String pm10CalcDate,
        @JsonProperty("pm10IndexLevel") GiosIndexLevelDto pm10IndexLevel,
        @JsonProperty("pm10SourceDataDate") String pm10SourceDataDate,

        @JsonProperty("pm25CalcDate") String pm25CalcDate,
        @JsonProperty("pm25IndexLevel") GiosIndexLevelDto pm25IndexLevel,
        @JsonProperty("pm25SourceDataDate") String pm25SourceDataDate,

        @JsonProperty("o3CalcDate") String o3CalcDate,
        @JsonProperty("o3IndexLevel") GiosIndexLevelDto o3IndexLevel,
        @JsonProperty("o3SourceDataDate") String o3SourceDataDate,

        @JsonProperty("no2CalcDate") String no2CalcDate,
        @JsonProperty("no2IndexLevel") GiosIndexLevelDto no2IndexLevel,
        @JsonProperty("no2SourceDataDate") String no2SourceDataDate,

        @JsonProperty("so2CalcDate") String so2CalcDate,
        @JsonProperty("so2IndexLevel") GiosIndexLevelDto so2IndexLevel,
        @JsonProperty("so2SourceDataDate") String so2SourceDataDate,

        @JsonProperty("coCalcDate") String coCalcDate,
        @JsonProperty("coIndexLevel") GiosIndexLevelDto coIndexLevel,
        @JsonProperty("coSourceDataDate") String coSourceDataDate,

        @JsonProperty("c6h6CalcDate") String c6h6CalcDate,
        @JsonProperty("c6h6IndexLevel") GiosIndexLevelDto c6h6IndexLevel,
        @JsonProperty("c6h6SourceDataDate") String c6h6SourceDataDate
) {}
