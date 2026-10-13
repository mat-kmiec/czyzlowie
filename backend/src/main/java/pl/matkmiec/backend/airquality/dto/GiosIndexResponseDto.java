package pl.matkmiec.backend.airquality.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GiosIndexResponseDto(
        @JsonProperty("id")
        @JsonAlias({"Identyfikator stacji pomiarowej", "id"})
        Long id,

        @JsonProperty("stCalcDate")
        @JsonAlias({"Data wykonania obliczeń indeksu", "stCalcDate"})
        String stCalcDate,

        @JsonProperty("stIndexLevel")
        @JsonAlias({"stIndexLevel"})
        GiosIndexLevelDto stIndexLevel,

        @JsonProperty("stIndexLevelId")
        @JsonAlias({"Wartość indeksu", "stIndexLevelId"})
        Integer stIndexLevelId,

        @JsonProperty("stIndexLevelName")
        @JsonAlias({"Nazwa kategorii indeksu", "stIndexLevelName"})
        String stIndexLevelName,

        @JsonProperty("stSourceDataDate")
        @JsonAlias({"Data danych źródłowych, z których policzono wartość indeksu dla wskaźnika st", "stSourceDataDate"})
        String stSourceDataDate,

        @JsonProperty("stIndexStatus")
        @JsonAlias({"Status indeksu ogólnego dla stacji pomiarowej", "stIndexStatus"})
        Boolean stIndexStatus,

        @JsonProperty("stIndexCrParam")
        @JsonAlias({"Kod zanieczyszczenia krytycznego", "stIndexCrParam"})
        String stIndexCrParam,

        @JsonProperty("pm10CalcDate")
        @JsonAlias({"Data wykonania obliczeń indeksu dla wskaźnika PM10", "pm10CalcDate"})
        String pm10CalcDate,

        @JsonProperty("pm10IndexLevel")
        @JsonAlias({"pm10IndexLevel"})
        GiosIndexLevelDto pm10IndexLevel,

        @JsonProperty("pm10IndexLevelName")
        @JsonAlias({"Nazwa kategorii indeksu dla wskażnika PM10", "Nazwa kategorii indeksu dla wskaźnika PM10", "pm10IndexLevelName"})
        String pm10IndexLevelName,

        @JsonProperty("pm10SourceDataDate")
        @JsonAlias({"Data danych źródłowych, z których policzono wartość indeksu dla wskaźnika PM10", "pm10SourceDataDate"})
        String pm10SourceDataDate,

        @JsonProperty("pm25CalcDate")
        @JsonAlias({"Data wykonania obliczeń indeksu dla wskaźnika PM2.5", "pm25CalcDate"})
        String pm25CalcDate,

        @JsonProperty("pm25IndexLevel")
        @JsonAlias({"pm25IndexLevel"})
        GiosIndexLevelDto pm25IndexLevel,

        @JsonProperty("pm25IndexLevelName")
        @JsonAlias({"Nazwa kategorii indeksu dla wskażnika PM2.5", "Nazwa kategorii indeksu dla wskaźnika PM2.5", "pm25IndexLevelName"})
        String pm25IndexLevelName,

        @JsonProperty("pm25SourceDataDate")
        @JsonAlias({"Data danych źródłowych, z których policzono wartość indeksu dla wskaźnika PM2.5", "pm25SourceDataDate"})
        String pm25SourceDataDate,

        @JsonProperty("o3CalcDate")
        @JsonAlias({"Data wykonania obliczeń indeksu dla wskaźnika O3", "o3CalcDate"})
        String o3CalcDate,

        @JsonProperty("o3IndexLevel")
        @JsonAlias({"o3IndexLevel"})
        GiosIndexLevelDto o3IndexLevel,

        @JsonProperty("o3IndexLevelName")
        @JsonAlias({"Nazwa kategorii indeksu dla wskażnika O3", "Nazwa kategorii indeksu dla wskaźnika O3", "o3IndexLevelName"})
        String o3IndexLevelName,

        @JsonProperty("o3SourceDataDate")
        @JsonAlias({"Data danych źródłowych, z których policzono wartość indeksu dla wskaźnika O3", "o3SourceDataDate"})
        String o3SourceDataDate,

        @JsonProperty("no2CalcDate")
        @JsonAlias({"Data wykonania obliczeń indeksu dla wskaźnika NO2", "no2CalcDate"})
        String no2CalcDate,

        @JsonProperty("no2IndexLevel")
        @JsonAlias({"no2IndexLevel"})
        GiosIndexLevelDto no2IndexLevel,

        @JsonProperty("no2IndexLevelName")
        @JsonAlias({"Nazwa kategorii indeksu dla wskażnika NO2", "Nazwa kategorii indeksu dla wskaźnika NO2", "no2IndexLevelName"})
        String no2IndexLevelName,

        @JsonProperty("no2SourceDataDate")
        @JsonAlias({"Data danych źródłowych, z których policzono wartość indeksu dla wskaźnika NO2", "no2SourceDataDate"})
        String no2SourceDataDate,

        @JsonProperty("so2CalcDate")
        @JsonAlias({"Data wykonania obliczeń indeksu dla wskaźnika SO2", "so2CalcDate"})
        String so2CalcDate,

        @JsonProperty("so2IndexLevel")
        @JsonAlias({"so2IndexLevel"})
        GiosIndexLevelDto so2IndexLevel,

        @JsonProperty("so2IndexLevelName")
        @JsonAlias({"Nazwa kategorii indeksu dla wskażnika SO2", "Nazwa kategorii indeksu dla wskaźnika SO2", "so2IndexLevelName"})
        String so2IndexLevelName,

        @JsonProperty("so2SourceDataDate")
        @JsonAlias({"Data danych źródłowych, z których policzono wartość indeksu dla wskaźnika SO2", "so2SourceDataDate"})
        String so2SourceDataDate,

        @JsonProperty("coCalcDate")
        @JsonAlias({"Data wykonania obliczeń indeksu dla wskaźnika CO", "coCalcDate"})
        String coCalcDate,

        @JsonProperty("coIndexLevel")
        @JsonAlias({"coIndexLevel"})
        GiosIndexLevelDto coIndexLevel,

        @JsonProperty("coIndexLevelName")
        @JsonAlias({"Nazwa kategorii indeksu dla wskażnika CO", "Nazwa kategorii indeksu dla wskaźnika CO", "coIndexLevelName"})
        String coIndexLevelName,

        @JsonProperty("coSourceDataDate")
        @JsonAlias({"Data danych źródłowych, z których policzono wartość indeksu dla wskaźnika CO", "coSourceDataDate"})
        String coSourceDataDate,

        @JsonProperty("c6h6CalcDate")
        @JsonAlias({"Data wykonania obliczeń indeksu dla wskaźnika C6H6", "c6h6CalcDate"})
        String c6h6CalcDate,

        @JsonProperty("c6h6IndexLevel")
        @JsonAlias({"c6h6IndexLevel"})
        GiosIndexLevelDto c6h6IndexLevel,

        @JsonProperty("c6h6IndexLevelName")
        @JsonAlias({"Nazwa kategorii indeksu dla wskażnika C6H6", "Nazwa kategorii indeksu dla wskaźnika C6H6", "c6h6IndexLevelName"})
        String c6h6IndexLevelName,

        @JsonProperty("c6h6SourceDataDate")
        @JsonAlias({"Data danych źródłowych, z których policzono wartość indeksu dla wskaźnika C6H6", "c6h6SourceDataDate"})
        String c6h6SourceDataDate
) {
    public GiosIndexResponseDto(
            Long id,
            String stCalcDate,
            GiosIndexLevelDto stIndexLevel,
            String stSourceDataDate,
            Boolean stIndexStatus,
            String stIndexCrParam,
            String pm10CalcDate,
            GiosIndexLevelDto pm10IndexLevel,
            String pm10SourceDataDate,
            String pm25CalcDate,
            GiosIndexLevelDto pm25IndexLevel,
            String pm25SourceDataDate,
            String o3CalcDate,
            GiosIndexLevelDto o3IndexLevel,
            String o3SourceDataDate,
            String no2CalcDate,
            GiosIndexLevelDto no2IndexLevel,
            String no2SourceDataDate,
            String so2CalcDate,
            GiosIndexLevelDto so2IndexLevel,
            String so2SourceDataDate,
            String coCalcDate,
            GiosIndexLevelDto coIndexLevel,
            String coSourceDataDate,
            String c6h6CalcDate,
            GiosIndexLevelDto c6h6IndexLevel,
            String c6h6SourceDataDate
    ) {
        this(
                id, stCalcDate, stIndexLevel, null, null, stSourceDataDate, stIndexStatus, stIndexCrParam,
                pm10CalcDate, pm10IndexLevel, null, pm10SourceDataDate,
                pm25CalcDate, pm25IndexLevel, null, pm25SourceDataDate,
                o3CalcDate, o3IndexLevel, null, o3SourceDataDate,
                no2CalcDate, no2IndexLevel, null, no2SourceDataDate,
                so2CalcDate, so2IndexLevel, null, so2SourceDataDate,
                coCalcDate, coIndexLevel, null, coSourceDataDate,
                c6h6CalcDate, c6h6IndexLevel, null, c6h6SourceDataDate
        );
    }
}
