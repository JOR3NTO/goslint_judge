package co.uceva.auth.dto;

/**
 * Par de tokens que devuelven {@code login} y {@code refresh}.
 *
 * @param expiresIn Vigencia del access token en milisegundos.
 */
public record LoginResponse(String accessToken, String refreshToken, String type, long expiresIn) {

    public static LoginResponse bearer(String accessToken, String refreshToken, long expiresIn) {
        return new LoginResponse(accessToken, refreshToken, "Bearer", expiresIn);
    }
}
