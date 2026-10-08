package pl.matkmiec.backend.weather.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "imgw_hydro_data")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImgwHydroData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id", nullable = false)
    private ImgwHydroStation station;

    @Column(name = "measurement_time", nullable = false)
    private LocalDateTime measurementTime;

    @Column(name = "water_level")
    private Double waterLevel;

    @Column(name = "water_temperature")
    private Double waterTemperature;

    @Column(name = "flow")
    private Double flow;

    @Column(name = "ice_phenomenon")
    private Integer icePhenomenon;

    @Column(name = "overgrowth_phenomenon")
    private Integer overgrowthPhenomenon;
}