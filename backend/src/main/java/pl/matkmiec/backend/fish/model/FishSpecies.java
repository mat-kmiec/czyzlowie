package pl.matkmiec.backend.fish.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "fish_species", indexes = {
        @Index(name = "idx_fish_species_type", columnList = "fish_type"),
        @Index(name = "idx_fish_species_legal_status", columnList = "legal_status"),
        @Index(name = "idx_fish_species_display_order", columnList = "display_order")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FishSpecies {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "slug", nullable = false, unique = true, length = 100)
    private String slug;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "latin_name", nullable = false, length = 100)
    private String latinName;

    @Column(name = "family", nullable = false, length = 100)
    private String family;

    @Enumerated(EnumType.STRING)
    @Column(name = "fish_type", nullable = false, length = 50)
    private FishType fishType;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "short_description", length = 500)
    private String shortDescription;

    @Column(name = "description", length = 4000)
    private String description;

    @Column(name = "identification_features", length = 4000)
    private String identificationFeatures;

    // II. Regulamin i status prawny
    @Column(name = "min_size_limit")
    private Integer minSizeLimit;

    @Column(name = "max_size_limit")
    private Integer maxSizeLimit;

    @Column(name = "protection_period", length = 255)
    private String protectionPeriod;

    @Column(name = "daily_limit", length = 255)
    private String dailyLimit;

    @Enumerated(EnumType.STRING)
    @Column(name = "legal_status", nullable = false, length = 50)
    private LegalStatus legalStatus;

    @Column(name = "legal_notes", length = 4000)
    private String legalNotes;

    // III. Biologia i rekordy
    @Column(name = "record_weight_kg")
    private Double recordWeightKg;

    @Column(name = "record_length_cm")
    private Double recordLengthCm;

    @Column(name = "record_year")
    private Integer recordYear;

    @Column(name = "record_angler", length = 150)
    private String recordAngler;

    @Column(name = "average_weight", length = 100)
    private String averageWeight;

    @Column(name = "average_length", length = 100)
    private String averageLength;

    @Column(name = "water_preferences", length = 4000)
    private String waterPreferences;

    @Enumerated(EnumType.STRING)
    @Column(name = "water_zone", length = 50)
    private WaterZone waterZone;

    @Column(name = "natural_food", length = 4000)
    private String naturalFood;

    @Column(name = "spawning_months", length = 100)
    private String spawningMonths;

    @Column(name = "active_months", length = 100)
    private String activeMonths;

    @Column(name = "similar_species", length = 4000)
    private String similarSpecies;

    // IV. Wskazówki wędkarskie (Taktyka)
    @Column(name = "daily_activity", length = 4000)
    private String dailyActivity;

    @Column(name = "seasonal_activity", length = 4000)
    private String seasonalActivity;

    @Column(name = "weather_preferences", length = 4000)
    private String weatherPreferences;

    @Column(name = "effective_methods", length = 4000)
    private String effectiveMethods;

    @Column(name = "best_artificial_baits", length = 4000)
    private String bestArtificialBaits;

    @Column(name = "best_natural_baits", length = 4000)
    private String bestNaturalBaits;

    @Column(name = "bite_and_fight_characteristics", length = 4000)
    private String biteAndFightCharacteristics;

    @Column(name = "typical_spots", length = 4000)
    private String typicalSpots;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", length = 50)
    private DifficultyLevel difficulty;

    // V. Moduł Sprzętowy (Parametry techniczne)
    @Column(name = "line_monofilament", length = 255)
    private String lineMonofilament;

    @Column(name = "line_braid", length = 255)
    private String lineBraid;

    @Column(name = "recommended_hook_size", length = 255)
    private String recommendedHookSize;

    @Column(name = "recommended_casting_weight", length = 255)
    private String recommendedCastingWeight;

    @Column(name = "recommended_rod_action", length = 255)
    private String recommendedRodAction;

    @Column(name = "leader_required")
    private Boolean leaderRequired;

    @Column(name = "recommended_leader", length = 1000)
    private String recommendedLeader;

    @Column(name = "additional_gear_tips", length = 4000)
    private String additionalGearTips;

    // VI. Moduł Catch & Release i Bezpieczeństwo
    @Column(name = "landing_method", length = 1000)
    private String landingMethod;

    @Column(name = "required_gear_c_and_r", length = 1000)
    private String requiredGearCAndR;

    @Column(name = "safety_alerts", length = 2000)
    private String safetyAlerts;

    @Column(name = "special_care_tips", length = 4000)
    private String specialCareTips;

    @Column(name = "barbless_hooks_recommended")
    private Boolean barblessHooksRecommended;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (isActive == null) {
            isActive = true;
        }
        if (displayOrder == null) {
            displayOrder = 0;
        }
        if (slug == null || slug.isBlank()) {
            slug = generateSlug(name);
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
        if (slug == null || slug.isBlank()) {
            slug = generateSlug(name);
        }
    }

    public static String generateSlug(String input) {
        if (input == null) return "";
        return input.toLowerCase()
                .trim()
                .replace("ą", "a")
                .replace("ć", "c")
                .replace("ę", "e")
                .replace("ł", "l")
                .replace("ń", "n")
                .replace("ó", "o")
                .replace("ś", "s")
                .replace("ź", "z")
                .replace("ż", "z")
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-");
    }
}
