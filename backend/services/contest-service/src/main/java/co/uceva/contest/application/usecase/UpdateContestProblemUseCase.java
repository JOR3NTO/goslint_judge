package co.uceva.contest.application.usecase;

import java.util.UUID;

import co.uceva.contest.domain.model.ContestProblem;

/**
 * Puerto de entrada para el caso de uso de actualización de un problema de concurso.
 */
public interface UpdateContestProblemUseCase {
    /**
     * Ejecuta la actualización de un problema de concurso existente.
     *
     * @param command Datos necesarios para actualizar el problema.
     * @return El problema de concurso actualizado.
     */
    ContestProblem execute(UpdateContestProblemCommand command);

    /**
     * Comando inmutable que agrupa los datos de entrada para actualizar un problema de concurso.
     *
     * @param contestProblemId Identificador del problema de concurso a actualizar.
     * @param alias            Nuevo alias visible.
     * @param orderIndex       Nueva posición.
     * @param points           Nuevos puntos.
     * @param balloonColor     Nuevo color del globo en formato {@code #RRGGBB}.
     */
    record UpdateContestProblemCommand(
        UUID contestProblemId,
        String alias,
        int orderIndex,
        int points,
        String balloonColor
    ){}
}
