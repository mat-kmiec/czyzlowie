package pl.matkmiec.backend.airquality.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "gios_station")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GiosStation {

    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "station_name", nullable = false)
    private String stationName;

    @Column(name = "gegr_lat", nullable = false)
    private Double gegrLat;

    @Column(name = "gegr_lon", nullable = false)
    private Double gegrLon;

    @Column(name = "address_street")
    private String addressStreet;

    @Column(name = "city_id")
    private Long cityId;

    @Column(name = "city_name")
    private String cityName;

    @Column(name = "commune_name")
    private String communeName;

    @Column(name = "district_name")
    private String districtName;

    @Column(name = "province_name")
    private String provinceName;

    @Column(name = "teryt_code", length = 20)
    private String terytCode;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;
}
