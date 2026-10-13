package pl.matkmiec.backend.fish.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FishRecordLeaderboardDto {
    private Long id;
    private String slug;
    private String name;
    private String latinName;
    private String family;
    private String fishType;
    private String imageUrl;
    private Double recordWeightKg;
    private Double recordLengthCm;
    private Integer recordYear;
    private String recordAngler;
}
