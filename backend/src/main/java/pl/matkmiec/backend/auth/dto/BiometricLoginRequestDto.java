package pl.matkmiec.backend.auth.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BiometricLoginRequestDto {
    private String refreshToken;
    private String deviceId;
}
