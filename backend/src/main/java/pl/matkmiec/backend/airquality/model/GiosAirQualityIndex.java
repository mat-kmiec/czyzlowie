package pl.matkmiec.backend.airquality.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "gios_air_quality_index")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GiosAirQualityIndex {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id", nullable = false, unique = true)
    private GiosStation station;

    @Column(name = "calculation_date")
    private LocalDateTime calculationDate;

    @Column(name = "source_data_date")
    private LocalDateTime sourceDataDate;

    @Column(name = "index_level_id")
    private Integer indexLevelId;

    @Column(name = "index_level_name", length = 100)
    private String indexLevelName;

    @Column(name = "st_index_status")
    private Boolean stIndexStatus;

    @Column(name = "st_index_cr_param", length = 50)
    private String stIndexCrParam;

    @Column(name = "pm10_index_level", length = 100)
    private String pm10IndexLevel;

    @Column(name = "pm10_source_data_date")
    private LocalDateTime pm10SourceDataDate;

    @Column(name = "pm25_index_level", length = 100)
    private String pm25IndexLevel;

    @Column(name = "pm25_source_data_date")
    private LocalDateTime pm25SourceDataDate;

    @Column(name = "o3_index_level", length = 100)
    private String o3IndexLevel;

    @Column(name = "o3_source_data_date")
    private LocalDateTime o3SourceDataDate;

    @Column(name = "no2_index_level", length = 100)
    private String no2IndexLevel;

    @Column(name = "no2_source_data_date")
    private LocalDateTime no2SourceDataDate;

    @Column(name = "so2_index_level", length = 100)
    private String so2IndexLevel;

    @Column(name = "so2_source_data_date")
    private LocalDateTime so2SourceDataDate;

    @Column(name = "co_index_level", length = 100)
    private String coIndexLevel;

    @Column(name = "co_source_data_date")
    private LocalDateTime coSourceDataDate;

    @Column(name = "c6h6_index_level", length = 100)
    private String c6h6IndexLevel;

    @Column(name = "c6h6_source_data_date")
    private LocalDateTime c6h6SourceDataDate;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
