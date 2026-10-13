package pl.matkmiec.backend.auth.dto;

import lombok.*;
import pl.matkmiec.backend.auth.model.PinType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResendPinRequestDto {
    private String email;
    @Builder.Default
    private PinType pinType = PinType.EMAIL_VERIFICATION;
}
