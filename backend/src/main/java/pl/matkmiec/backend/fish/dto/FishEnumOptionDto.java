package pl.matkmiec.backend.fish.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FishEnumOptionDto {
    private String code;
    private String displayName;
    private String description;
}
