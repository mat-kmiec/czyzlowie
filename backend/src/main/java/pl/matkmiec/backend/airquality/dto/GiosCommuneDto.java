package pl.matkmiec.backend.airquality.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GiosCommuneDto(
        @JsonProperty("communeName") String communeName,
        @JsonProperty("districtName") String districtName,
        @JsonProperty("provinceName") String provinceName
) {}
