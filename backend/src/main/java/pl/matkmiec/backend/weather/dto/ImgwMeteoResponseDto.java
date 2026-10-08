package pl.matkmiec.backend.weather.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ImgwMeteoResponseDto(
        @JsonProperty("kod_stacji") String kodStacji,
        @JsonProperty("nazwa_stacji") String nazwaStacji,
        @JsonProperty("lon") Double lon,
        @JsonProperty("lat") Double lat,
        @JsonProperty("rok_zalozenia_stacji") Integer rokZalozeniaStacji,
        @JsonProperty("wysokosc_npm") Integer wysokoscNpm,

        @JsonProperty("temperatura_gruntu") Double temperaturaGruntu,
        @JsonProperty("temperatura_gruntu_data") String temperaturaGruntuData,

        @JsonProperty("temperatura_powietrza") Double temperaturaPowietrza,
        @JsonProperty("temperatura_powietrza_data") String temperaturaPowietrzaData,

        @JsonProperty("wiatr_kierunek") Double wiatrKierunek,
        @JsonProperty("wiatr_kierunek_data") String wiatrKierunekData,

        @JsonProperty("wiatr_srednia_predkosc") Double wiatrSredniaPredkosc,
        @JsonProperty("wiatr_srednia_predkosc_data") String wiatrSredniaPredkoscData,

        @JsonProperty("wiatr_predkosc_maksymalna") Double wiatrPredkoscMaksymalna,
        @JsonProperty("wiatr_predkosc_maksymalna_data") String wiatrPredkoscMaksymalnaData,

        @JsonProperty("wilgotnosc_wzgledna") Double wilgotnoscWzgledna,
        @JsonProperty("wilgotnosc_wzgledna_data") String wilgotnoscWzglednaData,

        @JsonProperty("wiatr_poryw_10min") Double wiatrPoryw10min,
        @JsonProperty("wiatr_poryw_10min_data") String wiatrPoryw10minData,

        @JsonProperty("opad_10min") Double opad10min,
        @JsonProperty("opad_10min_data") String opad10minData
) {}
