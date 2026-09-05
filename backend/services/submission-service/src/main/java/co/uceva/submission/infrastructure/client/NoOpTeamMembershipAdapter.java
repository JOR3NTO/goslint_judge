package co.uceva.submission.infrastructure.client;

import co.uceva.submission.application.port.out.TeamDTO;
import co.uceva.submission.application.port.out.TeamMembershipPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Implementación provisional de {@link TeamMembershipPort} mientras
 * {@code contest-service} no exista.
 * <p>
 * Resuelve el equipo como <strong>individual</strong>: su único integrante es el
 * usuario cuyo identificador coincide con el del propio equipo. Es la convención
 * que ya sigue el envío en la práctica, donde un estudiante que resuelve
 * problemas por su cuenta envía a nombre de un "equipo" que es él mismo.
 * </p>
 * <p>
 * La suposición es deliberadamente conservadora de cara a la privacidad: nunca
 * amplía la lista de destinatarios más allá de un único usuario, de modo que
 * mientras esta implementación esté activa es imposible que un envío se notifique
 * a alguien que no sea su autor. Cuando {@code contest-service} publique la
 * composición real de los equipos, bastará con añadir un adaptador que consulte su
 * API y cambiar {@code app.team-membership.provider}, sin tocar nada de la capa de
 * aplicación.
 * </p>
 */
@Component
@ConditionalOnProperty(prefix = "app.team-membership", name = "provider", havingValue = "noop", matchIfMissing = true)
public class NoOpTeamMembershipAdapter implements TeamMembershipPort {

    private static final Logger log = LoggerFactory.getLogger(NoOpTeamMembershipAdapter.class);

    /**
     * Devuelve el equipo con un único integrante, el propio identificador del equipo.
     *
     * @param teamId Identificador del equipo a consultar.
     * @return Equipo individual con un solo integrante.
     */
    @Override
    public TeamDTO findTeam(UUID teamId) {
        log.debug("contest-service no disponible: el equipo {} se resuelve como individual.", teamId);
        return new TeamDTO(teamId, List.of(teamId));
    }
}
