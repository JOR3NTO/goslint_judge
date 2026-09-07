package co.uceva.auth.infrastructure.web.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private String type = "Bearer";
    private Long expiresIn;
}
