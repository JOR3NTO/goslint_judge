package co.uceva.contest.domain.repository;

import java.util.List;
import java.util.UUID;

import co.uceva.contest.domain.model.Member;

/**
 * Puerto de salida (contrato de repositorio) para la entidad {@link Member}.
 * Define las operaciones de persistencia para los integrantes de equipos.
 */
public interface MemberRepository {

    /**
     * Guarda un integrante y retorna la instancia persistida.
     *
     * @param member Integrante a persistir.
     * @return El integrante persistido.
     */
    Member save(Member member);

    /**
     * Guarda una lista de integrantes de forma masiva.
     *
     * @param members Lista de integrantes a persistir.
     * @return Lista de integrantes persistidos.
     */
    List<Member> saveAll(List<Member> members);

    /**
     * Recupera todos los integrantes asociados a un equipo específico.
     *
     * @param teamId Identificador del equipo.
     * @return Lista de miembros del equipo.
     */
    List<Member> findAllByTeamId(UUID teamId);

    /**
     * Recupera todas las membresías asociadas a un usuario específico.
     *
     * @param userId Identificador del usuario.
     * @return Lista de membresías del usuario en distintos equipos.
     */
    List<Member> findAllByUserId(UUID userId);

    /**
     * Verifica si un usuario ya es miembro de un equipo específico.
     *
     * @param teamId Identificador del equipo.
     * @param userId Identificador del usuario.
     * @return {@code true} si el usuario es miembro del equipo; {@code false} en caso contrario.
     */
    boolean existsByTeamIdAndUserId(UUID teamId, UUID userId);

    /**
     * Elimina un integrante específico de un equipo.
     *
     * @param teamId Identificador del equipo.
     * @param userId Identificador del usuario a remover.
     */
    void deleteByTeamIdAndUserId(UUID teamId, UUID userId);

    /**
     * Elimina todos los integrantes asociados a un equipo.
     *
     * @param teamId Identificador del equipo.
     */
    void deleteAllByTeamId(UUID teamId);
}

