package pl.matkmiec.backend.auth.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResetPasswordRequestDto {
    private String email;
    private String pin;
    private String newPassword;
}
