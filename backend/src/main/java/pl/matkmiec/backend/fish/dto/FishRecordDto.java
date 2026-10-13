package pl.matkmiec.backend.fish.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FishRecordDto {
    private Double weightKg;
    private Double lengthCm;
    private Integer year;
    private String angler;
}
