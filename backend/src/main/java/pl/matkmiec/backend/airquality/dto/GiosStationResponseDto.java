package pl.matkmiec.backend.airquality.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GiosStationResponseDto(
        @JsonProperty("id") Long id,
        @JsonProperty("stationName") String stationName,
        @JsonProperty("gegrLat") Double gegrLat,
        @JsonProperty("gegrLon") Double gegrLon,
        @JsonProperty("city") GiosCityDto city,
        @JsonProperty("addressStreet") String addressStreet
) {}
