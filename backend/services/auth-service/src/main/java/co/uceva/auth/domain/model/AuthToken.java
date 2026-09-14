package co.uceva.auth.domain.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthToken {
    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
}
