package pl.matkmiec.backend.moon.model;

import jakarta.persistence.*;
import lombok.*;
import pl.matkmiec.backend.moon.dto.SolunarActivityLevel;
import pl.matkmiec.backend.weather.model.ImgwSynopStation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "moon_station_day",
       uniqueConstraints = @UniqueConstraint(name = "uq_moon_station_day_station_date", columnNames = {"station_id", "calculation_date"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MoonStationDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id", nullable = false)
    private ImgwSynopStation station;

    @Column(name = "calculation_date", nullable = false)
    private LocalDate calculationDate;

    // Sun times
    @Column(name = "sunrise")
    private LocalTime sunrise;

    @Column(name = "sunset")
    private LocalTime sunset;

    @Column(name = "solar_noon")
    private LocalTime solarNoon;

    @Column(name = "day_length_seconds")
    private Long dayLengthSeconds;

    @Column(name = "day_length_formatted", length = 50)
    private String dayLengthFormatted;

    @Column(name = "day_length_diff_seconds")
    private Long dayLengthDiffSeconds;

    @Column(name = "morning_golden_hour_start")
    private LocalTime morningGoldenHourStart;

    @Column(name = "morning_golden_hour_end")
    private LocalTime morningGoldenHourEnd;

    @Column(name = "evening_golden_hour_start")
    private LocalTime eveningGoldenHourStart;

    @Column(name = "evening_golden_hour_end")
    private LocalTime eveningGoldenHourEnd;

    @Column(name = "morning_blue_hour_start")
    private LocalTime morningBlueHourStart;

    @Column(name = "morning_blue_hour_end")
    private LocalTime morningBlueHourEnd;

    @Column(name = "evening_blue_hour_start")
    private LocalTime eveningBlueHourStart;

    @Column(name = "evening_blue_hour_end")
    private LocalTime eveningBlueHourEnd;

    @Column(name = "civil_dawn")
    private LocalTime civilDawn;

    @Column(name = "civil_dusk")
    private LocalTime civilDusk;

    @Column(name = "nautical_dawn")
    private LocalTime nauticalDawn;

    @Column(name = "nautical_dusk")
    private LocalTime nauticalDusk;

    @Column(name = "astronomical_dawn")
    private LocalTime astronomicalDawn;

    @Column(name = "astronomical_dusk")
    private LocalTime astronomicalDusk;

    // Moon times
    @Column(name = "moonrise")
    private LocalTime moonrise;

    @Column(name = "moonset")
    private LocalTime moonset;

    @Column(name = "moon_transit")
    private LocalTime moonTransit;

    @Column(name = "moon_underfoot")
    private LocalTime moonUnderfoot;

    @Column(name = "moon_altitude_deg")
    private Double moonAltitudeDeg;

    @Column(name = "moon_azimuth_deg")
    private Double moonAzimuthDeg;

    // Solunar
    @Enumerated(EnumType.STRING)
    @Column(name = "solunar_rating", length = 50)
    private SolunarActivityLevel solunarRating;

    @Column(name = "solunar_score")
    private Integer solunarScore;

    @Column(name = "solunar_stars")
    private Integer solunarStars;

    @Column(name = "solunar_summary", length = 500)
    private String solunarSummary;

    @Column(name = "moon_phase_influence", length = 500)
    private String moonPhaseInfluence;

    @Column(name = "fishing_tip", length = 500)
    private String fishingTip;

    @Column(name = "major_period1_start")
    private LocalTime majorPeriod1Start;

    @Column(name = "major_period1_end")
    private LocalTime majorPeriod1End;

    @Column(name = "major_period2_start")
    private LocalTime majorPeriod2Start;

    @Column(name = "major_period2_end")
    private LocalTime majorPeriod2End;

    @Column(name = "minor_period1_start")
    private LocalTime minorPeriod1Start;

    @Column(name = "minor_period1_end")
    private LocalTime minorPeriod1End;

    @Column(name = "minor_period2_start")
    private LocalTime minorPeriod2Start;

    @Column(name = "minor_period2_end")
    private LocalTime minorPeriod2End;

    @Column(name = "peak_windows", length = 500)
    private String peakWindows;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
