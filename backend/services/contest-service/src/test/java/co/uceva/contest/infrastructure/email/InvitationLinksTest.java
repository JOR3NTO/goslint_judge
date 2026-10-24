package co.uceva.contest.infrastructure.email;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Enlaces firmados del correo de invitación")
class InvitationLinksTest {

    private final InvitationLinks links = new InvitationLinks("0123456789abcdef0123456789abcdef", "http://api.test/");

    @Test
    @DisplayName("El token del enlace es válido solo para su invitación")
    void tokenIsBoundToInvitation() {
        UUID id = UUID.randomUUID();
        String token = links.token(id);

        assertThat(links.isValid(id, token)).isTrue();
        assertThat(links.isValid(UUID.randomUUID(), token)).isFalse();
        assertThat(links.isValid(id, token + "x")).isFalse();
        assertThat(links.isValid(id, null)).isFalse();
    }

    @Test
    @DisplayName("La URL apunta al endpoint de respuesta con acción y token")
    void buildsRespondUrl() {
        UUID id = UUID.randomUUID();

        assertThat(links.respondUrl(id, "accept"))
                .isEqualTo("http://api.test/api/v1/invitations/" + id + "/respond?action=accept&token=" + links.token(id));
    }
}
