package pl.matkmiec.backend.airquality.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GiosStationResponseDto(
        @JsonProperty("id")
        @JsonAlias({"Identyfikator stacji", "id"})
        Long id,

        @JsonProperty("stationName")
        @JsonAlias({"Nazwa stacji", "stationName"})
        String stationName,

        @JsonProperty("gegrLat")
        @JsonAlias({"WGS84 φ N", "gegrLat", "lat", "latitude"})
        Double gegrLat,

        @JsonProperty("gegrLon")
        @JsonAlias({"WGS84 λ E", "gegrLon", "lon", "longitude"})
        Double gegrLon,

        @JsonProperty("city")
        @JsonAlias({"city"})
        GiosCityDto city,

        @JsonProperty("addressStreet")
        @JsonAlias({"Ulica", "addressStreet"})
        String addressStreet,

        @JsonProperty("cityId")
        @JsonAlias({"Identyfikator miasta", "cityId"})
        Long cityId,

        @JsonProperty("cityName")
        @JsonAlias({"Nazwa miasta", "cityName"})
        String cityName,

        @JsonProperty("communeName")
        @JsonAlias({"Gmina", "communeName"})
        String communeName,

        @JsonProperty("districtName")
        @JsonAlias({"Powiat", "districtName"})
        String districtName,

        @JsonProperty("provinceName")
        @JsonAlias({"Województwo", "provinceName"})
        String provinceName
) {
    public GiosStationResponseDto(Long id, String stationName, Double gegrLat, Double gegrLon, GiosCityDto city, String addressStreet) {
        this(id, stationName, gegrLat, gegrLon, city, addressStreet, null, null, null, null, null);
    }
}
