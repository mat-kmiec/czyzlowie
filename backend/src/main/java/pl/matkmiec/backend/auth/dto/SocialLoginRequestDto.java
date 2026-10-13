package pl.matkmiec.backend.auth.dto;

import lombok.*;
import pl.matkmiec.backend.user.model.AuthProvider;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SocialLoginRequestDto {
    private AuthProvider provider;
    private String idToken;
    private String authorizationCode;
    private String nonce;
    private String email;
    private String fullName;
}
