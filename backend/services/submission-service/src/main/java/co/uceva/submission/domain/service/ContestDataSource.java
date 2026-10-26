package co.uceva.submission.domain.service;

import co.uceva.submission.domain.model.ContestSnapshot;
import co.uceva.submission.domain.model.TeamSnapshot;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Fuente de los datos de concursos y equipos que necesita la política de
 * elegibilidad.
 * <p>
 * Se declara en el dominio, y no se pasa la información ya cargada, para que la
 * política consulte cada dato solo cuando le hace falta: en cuanto una regla
 * falla, no se hacen más llamadas remotas. La implementación real es el puerto de
 * salida de la capa de aplicación, que extiende esta interfaz.
 * </p>
 */
public interface ContestDataSource {

    /**
     * @param teamId Identificador del equipo.
     * @return El equipo, o vacío si no existe.
     */
    Optional<TeamSnapshot> findTeam(UUID teamId);

    /**
     * @param contestId Identificador del concurso.
     * @return El concurso, o vacío si no existe.
     */
    Optional<ContestSnapshot> findContest(UUID contestId);

    /**
     * @param contestId Identificador del concurso.
     * @return Identificadores de los problemas del concurso (vacía si no tiene o no existe).
     */
    List<UUID> findContestProblemIds(UUID contestId);
}
