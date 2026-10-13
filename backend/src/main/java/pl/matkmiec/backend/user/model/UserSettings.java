package pl.matkmiec.backend.user.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSettings {

    @Id
    @Column(name = "user_id", length = 36, nullable = false)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "push_notifications_enabled", nullable = false)
    @Builder.Default
    private Boolean pushNotificationsEnabled = true;

    @Column(name = "web_push_enabled", nullable = false)
    @Builder.Default
    private Boolean webPushEnabled = true;

    @Column(name = "email_notifications_enabled", nullable = false)
    @Builder.Default
    private Boolean emailNotificationsEnabled = true;

    @Column(name = "hide_exact_gps_coordinates", nullable = false)
    @Builder.Default
    private Boolean hideExactGpsCoordinates = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "profile_visibility", length = 50, nullable = false)
    @Builder.Default
    private ProfileVisibility profileVisibility = ProfileVisibility.PUBLIC;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (pushNotificationsEnabled == null) {
            pushNotificationsEnabled = true;
        }
        if (webPushEnabled == null) {
            webPushEnabled = true;
        }
        if (emailNotificationsEnabled == null) {
            emailNotificationsEnabled = true;
        }
        if (hideExactGpsCoordinates == null) {
            hideExactGpsCoordinates = true;
        }
        if (profileVisibility == null) {
            profileVisibility = ProfileVisibility.PUBLIC;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
