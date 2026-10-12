package pl.matkmiec.backend.airquality.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GiosCityDto(
        @JsonProperty("id") Long id,
        @JsonProperty("name") String name,
        @JsonProperty("commune") GiosCommuneDto commune
) {}
