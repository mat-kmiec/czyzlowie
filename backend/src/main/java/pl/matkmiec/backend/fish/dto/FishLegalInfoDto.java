package pl.matkmiec.backend.fish.dto;

import lombok.*;
import pl.matkmiec.backend.fish.model.LegalStatus;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FishLegalInfoDto {
    private Integer minSizeLimit;
    private Integer maxSizeLimit;
    private String protectionPeriod;
    private String dailyLimit;
    private LegalStatus legalStatus;
    private String legalStatusDisplayName;
    private String legalStatusDescription;
    private String legalNotes;
    private Boolean isProtectedNow;
}
