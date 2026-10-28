package co.uceva.contest.application.usecase;

import java.util.UUID;

/**
 * Puerto de entrada para el caso de uso de eliminación de un concurso.
 */
public interface DeleteContestUseCase {
    /**
     * Ejecuta la eliminación de un concurso.
     *
     * @param contestId Identificador del concurso a eliminar.
     */
    void execute(UUID contestId);
}
