package pl.matkmiec.backend.auth.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerifyResetPinRequestDto {
    private String email;
    private String pin;
}
