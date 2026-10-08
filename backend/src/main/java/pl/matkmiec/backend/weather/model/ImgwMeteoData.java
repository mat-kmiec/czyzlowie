package pl.matkmiec.backend.weather.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "imgw_meteo_data")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImgwMeteoData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id", nullable = false)
    private ImgwMeteoStation station;

    @Column(name = "measurement_time", nullable = false)
    private LocalDateTime measurementTime;

    @Column(name = "ground_temperature")
    private Double groundTemperature;

    @Column(name = "air_temperature")
    private Double airTemperature;

    @Column(name = "wind_speed")
    private Double windSpeed;

    @Column(name = "wind_direction")
    private Integer windDirection;

    @Column(name = "wind_gust_10min")
    private Double windGust10min;

    @Column(name = "relative_humidity")
    private Double relativeHumidity;

    @Column(name = "precipitation_10min")
    private Double precipitation10min;
}
