package pl.matkmiec.backend.airquality.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "air_quality_sync_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AirQualitySyncLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider_name", nullable = false, length = 100)
    private String providerName;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "records_processed")
    private Integer recordsProcessed;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "error_message", length = 4000)
    private String errorMessage;
}
