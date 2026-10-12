package pl.matkmiec.backend.airquality.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "gios_sensor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GiosSensor {

    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id", nullable = false)
    private GiosStation station;

    @Column(name = "param_name", nullable = false)
    private String paramName;

    @Column(name = "param_formula", nullable = false, length = 50)
    private String paramFormula;

    @Column(name = "param_code", nullable = false, length = 50)
    private String paramCode;

    @Column(name = "id_param")
    private Integer idParam;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
