package pl.matkmiec.backend.weather.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "open_meteo_forecast")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpenMeteoForecast {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "synop_station_id", nullable = false)
    private ImgwSynopStation synopStation;

    @Column(name = "forecast_time", nullable = false)
    private LocalDateTime forecastTime;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

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