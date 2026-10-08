package pl.matkmiec.backend.weather.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "imgw_synop_data")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImgwSynopData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id", nullable = false)
    private ImgwSynopStation station;

    @Column(name = "measurement_time", nullable = false)
    private LocalDateTime measurementTime;

    @Column(name = "temperature")
    private Double temperature;

    @Column(name = "wind_speed")
    private Double windSpeed;

    @Column(name = "wind_direction")
    private Integer windDirection;

    @Column(name = "relative_humidity")
    private Double relativeHumidity;

    @Column(name = "precipitation")
    private Double precipitation;

    @Column(name = "pressure")
    private Double pressure;
}
