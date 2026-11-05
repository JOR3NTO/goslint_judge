package co.uceva.contest.infrastructure.email;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Genera y verifica los enlaces de aceptar/rechazar que viajan en el correo de invitación.
 * El token es un HMAC del ID de la invitación: solo quien recibió el correo puede responder,
 * aunque el ID sea visible para el capitán.
 */
@Component
public class InvitationLinks {

    private final SecretKeySpec key;
    private final String publicUrl;

    public InvitationLinks(@Value("${app.security.jwt.secret}") String secret,
                           @Value("${app.public-url}") String publicUrl) {
        this.key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.publicUrl = publicUrl.replaceAll("/+$", "");
    }

    public String respondUrl(UUID invitationId, String action) {
        return publicUrl + "/api/v1/invitations/" + invitationId + "/respond?action=" + action
                + "&token=" + token(invitationId);
    }

    public boolean isValid(UUID invitationId, String token) {
        return token != null && MessageDigest.isEqual(
                token(invitationId).getBytes(StandardCharsets.US_ASCII), token.getBytes(StandardCharsets.US_ASCII));
    }

    String token(UUID invitationId) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            // El prefijo separa este uso de la clave del de firmar JWT.
            byte[] digest = mac.doFinal(("team-invitation:" + invitationId).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo firmar el enlace de invitación.", e);
        }
    }
}
