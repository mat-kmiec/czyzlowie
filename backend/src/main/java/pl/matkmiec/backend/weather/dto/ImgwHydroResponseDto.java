package pl.matkmiec.backend.weather.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ImgwHydroResponseDto(
        @JsonProperty("id_stacji") String idStacji,
        @JsonProperty("stacja") String stacja,
        @JsonProperty("rzeka") String rzeka,
        @JsonProperty("wojewodztwo") String wojewodztwo,
        @JsonProperty("lat") Double lat,
        @JsonProperty("lon") Double lon,
        @JsonProperty("rok_zalozenia_stacji") Integer rokZalozeniaStacji,
        @JsonProperty("rzedna_zerawodowskazu") Double rzednaZeraWodowskazu,
        @JsonProperty("kilometr_biegu_rzeki") Double kilometrBieguRzeki,
        @JsonProperty("stan_alarmowy") Double stanAlarmowy,
        @JsonProperty("stan_ostrzegawczy") Double stanOstrzegawczy,
        @JsonProperty("stan_wody") Double stanWody,
        @JsonProperty("stan_wody_data_pomiaru") String stanWodyDataPomiaru,
        @JsonProperty("temperatura_wody") Double temperaturaWody,
        @JsonProperty("temperatura_wody_data_pomiaru") String temperaturaWodyDataPomiaru,
        @JsonProperty("przeplyw") Double przeplyw,
        @JsonProperty("przeplyw_data") String przeplywData,
        @JsonProperty("zjawisko_lodowe") Integer zjawiskoLodowe,
        @JsonProperty("zjawisko_lodowe_data_pomiaru") String zjawiskoLodoweDataPomiaru,
        @JsonProperty("zjawisko_zarastania") Integer zjawiskoZarastania,
        @JsonProperty("zjawisko_zarastania_data_pomiaru") String zjawiskoZarastaniaDataPomiaru
) {}
