package co.uceva.contest.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import co.uceva.contest.domain.model.Team;

/**
 * Puerto de salida (contrato de repositorio) para la entidad {@link Team}.
 * Define las operaciones de persistencia sin acoplarse a una tecnología específica.
 */
public interface TeamRepository {

    /**
     * Guarda un equipo y retorna la instancia persistida.
     *
     * @param team Equipo a persistir.
     * @return El equipo persistido.
     */
    Team save(Team team);

    /**
     * Busca un equipo por su identificador único universal.
     *
     * @param teamId Identificador del equipo.
     * @return {@link Optional} con el equipo si existe, o vacío en caso contrario.
     */
    Optional<Team> findById(UUID teamId);

    /**
     * Elimina un equipo dado su identificador único.
     *
     * @param teamId Identificador del equipo a eliminar.
     */
    void deleteById(UUID teamId);

    /**
     * Recupera todos los equipos registrados en la plataforma.
     *
     * @return Lista de todos los equipos.
     */
    List<Team> findAll();

    /**
     * Recupera todos los equipos inscritos en un concurso específico.
     *
     * @param contestId Identificador del concurso.
     * @return Lista de equipos pertenecientes a dicho concurso.
     */
    List<Team> findAllByContestId(UUID contestId);

    /**
     * Recupera los equipos de un concurso ordenados para la tabla de posiciones (scoreboard):
     * mayor puntaje primero (descendente) y menor tiempo de penalización (ascendente).
     *
     * @param contestId Identificador del concurso.
     * @return Lista de equipos clasificados para el scoreboard.
     */
    List<Team> findAllByContestIdOrderByScoreDescPenaltyTimeAsc(UUID contestId);

    /**
     * Busca equipos dentro de un concurso cuyo nombre contenga el fragmento dado,
     * sin distinguir mayúsculas de minúsculas.
     *
     * @param contestId Identificador del concurso.
     * @param nameTeam  Fragmento del nombre del equipo.
     * @return Lista de equipos coincidentes.
     */
    List<Team> findAllByContestIdAndNameTeamContaining(UUID contestId, String nameTeam);

    /**
     * Busca un equipo por su nombre exacto dentro de un concurso específico.
     *
     * @param contestId Identificador del concurso.
     * @param nameTeam  Nombre exacto del equipo.
     * @return {@link Optional} con el equipo si existe.
     */
    Optional<Team> findByContestIdAndNameTeam(UUID contestId, String nameTeam);

    /**
     * Verifica si ya existe un equipo con el nombre indicado en un concurso específico.
     *
     * @param contestId Identificador del concurso.
     * @param nameTeam  Nombre del equipo a comprobar.
     * @return {@code true} si existe un equipo con ese nombre en el concurso; {@code false} en caso contrario.
     */
    boolean existsByContestIdAndNameTeam(UUID contestId, String nameTeam);

    /**
     * Verifica si existe un equipo dado su identificador único.
     *
     * @param teamId Identificador del equipo.
     * @return {@code true} si existe el equipo; {@code false} en caso contrario.
     */
    boolean existsById(UUID teamId);

    /**
     * Busca los equipos a los que pertenece un usuario específico a través de sus membresías.
     *
     * @param userId Identificador del usuario.
     * @return Lista de equipos donde el usuario es integrante.
     */
    List<Team> findAllByUserId(UUID userId);

    /**
     * Verifica si un usuario ya pertenece a algún equipo dentro de un concurso determinado.
     *
     * @param contestId Identificador del concurso.
     * @param userId    Identificador del usuario.
     * @return {@code true} si el usuario ya está inscrito en un equipo de dicho concurso.
     */
    boolean existsByContestIdAndUserId(UUID contestId, UUID userId);

    /**
     * Busca el equipo al que pertenece un usuario dentro de un concurso determinado.
     *
     * @param contestId Identificador del concurso.
     * @param userId    Identificador del usuario.
     * @return {@link Optional} con el equipo al que pertenece el usuario en dicho concurso.
     */
    Optional<Team> findByContestIdAndUserId(UUID contestId, UUID userId);
}

