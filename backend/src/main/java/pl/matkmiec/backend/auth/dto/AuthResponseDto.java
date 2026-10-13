package pl.matkmiec.backend.auth.dto;

import lombok.*;
import pl.matkmiec.backend.user.dto.UserProfileDto;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponseDto {
    private String accessToken;
    private String refreshToken;
    @Builder.Default
    private String tokenType = "Bearer";
    private Long expiresIn;
    private UserProfileDto user;
}
