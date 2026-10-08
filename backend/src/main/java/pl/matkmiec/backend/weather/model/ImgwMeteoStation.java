package pl.matkmiec.backend.weather.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "imgw_meteo_station")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImgwMeteoStation {

    @Id
    @Column(name = "id", nullable = false, length = 50)
    private String id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "lat")
    private Double lat;

    @Column(name = "lon")
    private Double lon;

    @Column(name = "elevation_asl")
    private Integer elevationAsl;

    @Column(name = "established_year")
    private Integer establishedYear;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
