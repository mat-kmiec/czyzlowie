package pl.matkmiec.backend.airquality.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "gios_measurement")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GiosMeasurement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sensor_id", nullable = false)
    private GiosSensor sensor;

    @Column(name = "station_id", nullable = false)
    private Long stationId;

    @Column(name = "param_code", nullable = false, length = 50)
    private String paramCode;

    @Column(name = "measurement_time", nullable = false)
    private LocalDateTime measurementTime;

    @Column(name = "value")
    private Double value;
}
