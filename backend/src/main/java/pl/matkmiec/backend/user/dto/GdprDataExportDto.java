package pl.matkmiec.backend.user.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GdprDataExportDto {
    private UUID userId;
    private String email;
    private String nickname;
    private String role;
    private String authProvider;
    private LocalDateTime accountCreatedAt;
    private LocalDateTime exportGeneratedAt;
    private String exportVersion;
    private UserSettingsDto settings;
    private Map<String, Object> consentsAndPrivacy;
    private List<Map<String, Object>> activeSessions;
    private List<Map<String, Object>> fishingDiaryCatches;
    private Map<String, Object> metadata;
}
