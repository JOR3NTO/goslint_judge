package co.uceva.submission.application.port.out;

import java.util.List;
import java.util.UUID;

/**
 * Datos de un equipo tal y como los necesita este servicio: quién lo compone.
 * <p>
 * Es deliberadamente mínimo. Un envío se registra a nombre de un equipo, pero la
 * notificación se entrega a personas, así que lo único que hace falta importar
 * desde {@code contest-service} es la lista de sus integrantes. El nombre del
 * equipo, su concurso o su contraseña no intervienen aquí y por tanto no se
 * copian.
 * </p>
 *
 * @param teamId        Identificador del equipo consultado.
 * @param memberUserIds Identificadores de los usuarios que pertenecen al equipo.
 */
public record TeamDTO(UUID teamId, List<UUID> memberUserIds) {}
