package pl.matkmiec.backend.moon.model;

import jakarta.persistence.*;
import lombok.*;
import pl.matkmiec.backend.moon.dto.MoonPhase;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "moon_general_day")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MoonGeneralDay {

    @Id
    @Column(name = "calculation_date", nullable = false)
    private LocalDate calculationDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "phase_name", nullable = false, length = 50)
    private MoonPhase phaseName;

    @Column(name = "phase_polish_name", length = 100)
    private String phasePolishName;

    @Column(name = "phase_english_name", length = 100)
    private String phaseEnglishName;

    @Column(name = "phase_icon_code", length = 50)
    private String phaseIconCode;

    @Column(name = "phase_weather_icon", length = 50)
    private String phaseWeatherIcon;

    @Column(name = "phase_value")
    private Double phaseValue;

    @Column(name = "illumination_percentage")
    private Double illuminationPercentage;

    @Column(name = "moon_age_days")
    private Double moonAgeDays;

    @Column(name = "distance_km")
    private Double distanceKm;

    @Column(name = "is_perigee")
    private Boolean isPerigee;

    @Column(name = "is_apogee")
    private Boolean isApogee;

    @Column(name = "distance_phenomenon", length = 100)
    private String distancePhenomenon;

    @Column(name = "next_new_moon")
    private LocalDate nextNewMoon;

    @Column(name = "next_first_quarter")
    private LocalDate nextFirstQuarter;

    @Column(name = "next_full_moon")
    private LocalDate nextFullMoon;

    @Column(name = "next_last_quarter")
    private LocalDate nextLastQuarter;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
