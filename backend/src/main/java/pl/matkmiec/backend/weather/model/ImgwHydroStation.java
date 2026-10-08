package pl.matkmiec.backend.weather.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "imgw_hydro_station")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImgwHydroStation {

    @Id
    @Column(name = "id", nullable = false, length = 50)
    private String id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "river")
    private String river;

    @Column(name = "province")
    private String province;

    @Column(name = "lat")
    private Double lat;

    @Column(name = "lon")
    private Double lon;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
