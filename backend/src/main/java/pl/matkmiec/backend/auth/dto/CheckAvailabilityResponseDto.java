package pl.matkmiec.backend.auth.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckAvailabilityResponseDto {
    private boolean available;
    private String message;
}
