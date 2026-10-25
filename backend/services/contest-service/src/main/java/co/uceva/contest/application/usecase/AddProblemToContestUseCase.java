package co.uceva.contest.application.usecase;

import java.util.UUID;

import co.uceva.contest.domain.model.ContestProblem;

/**
 * Puerto de entrada para el caso de uso de asignación de un problema a un concurso.
 */
public interface AddProblemToContestUseCase {
    /**
     * Ejecuta la asignación de un problema a un concurso.
     *
     * @param command Datos necesarios para asignar el problema.
     * @return El problema de concurso creado.
     */
    ContestProblem execute(AddProblemToContestCommand command);

    /**
     * Comando inmutable que agrupa los datos de entrada para asignar un problema a un concurso.
     *
     * @param contestId    Identificador del concurso.
     * @param problemId    Identificador del problema en problem-service.
     * @param alias        Alias visible del problema (A, B, C...).
     * @param orderIndex   Posición del problema en el concurso.
     * @param points       Puntos que otorga resolver el problema.
     * @param penalty Minutos de penalización por cada intento fallido.
     * @param balloonColor Color del globo en formato {@code #RRGGBB}.
     */
    record AddProblemToContestCommand(
        UUID contestId,
        UUID problemId,
        String alias,
        int orderIndex,
        int points,
        int penalty,
        String balloonColor
    ){}
}
