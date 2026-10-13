package pl.matkmiec.backend.fish.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FishBasicInfoDto {
    private String name;
    private String latinName;
    private String family;
    private String imageUrl;
    private String thumbnailUrl;
    private String shortDescription;
    private String description;
    private String identificationFeatures;
}
