package pl.matkmiec.backend.airquality.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "ios_air_quality_forecast")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IosAirQualityForecast {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "teryt_code", nullable = false, length = 20)
    private String terytCode;

    @Column(name = "pollutant", nullable = false, length = 50)
    private String pollutant;

    @Column(name = "forecast_date", nullable = false)
    private LocalDate forecastDate;

    @Column(name = "forecast_value", nullable = false)
    private Double forecastValue;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
