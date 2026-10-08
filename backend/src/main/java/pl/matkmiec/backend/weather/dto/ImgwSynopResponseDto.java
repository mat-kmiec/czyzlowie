package pl.matkmiec.backend.weather.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ImgwSynopResponseDto(
        @JsonProperty("id_stacji") String idStacji,
        @JsonProperty("stacja") String stacja,
        @JsonProperty("data_pomiaru") String dataPomiaru,
        @JsonProperty("godzina_pomiaru") String godzinaPomiaru,
        @JsonProperty("temperatura") Double temperatura,
        @JsonProperty("predkosc_wiatru") Double predkoscWiatru,
        @JsonProperty("kierunek_wiatru") Integer kierunekWiatru,
        @JsonProperty("wilgotnosc_wzgledna") Double wilgotnoscWzgledna,
        @JsonProperty("suma_opadu") Double sumaOpadu,
        @JsonProperty("cisnienie") Double cisnienie
) {}
