package co.uceva.submission.domain.model;

import java.util.List;
import java.util.UUID;

/**
 * Instantánea de solo lectura de un equipo, tal como la conoce {@code contest-service}.
 * <p>
 * {@code submission-service} no es dueño de los equipos: solo necesita saber a qué
 * concurso pertenecen y quiénes los integran para decidir si un envío es legítimo.
 * Por eso se modela como un valor inmutable y no como una entidad.
 * </p>
 *
 * @param teamId        Identificador del equipo.
 * @param contestId     Identificador del concurso en el que está inscrito el equipo.
 * @param memberUserIds Identificadores de los usuarios que integran el equipo.
 */
public record TeamSnapshot(UUID teamId, UUID contestId, List<UUID> memberUserIds) {

    /** Copia defensiva: la instantánea no debe cambiar si cambia la lista original. */
    public TeamSnapshot {
        memberUserIds = memberUserIds == null ? List.of() : List.copyOf(memberUserIds);
    }

    /**
     * @param userId Identificador del usuario a comprobar.
     * @return {@code true} si el usuario es integrante del equipo.
     */
    public boolean hasMember(UUID userId) {
        return userId != null && memberUserIds.contains(userId);
    }
}
