package co.uceva.contest.application.usecase;

import java.util.UUID;

/**
 * Puerto de entrada para el caso de uso de retiro de un problema de un concurso.
 */
public interface RemoveProblemFromContestUseCase {
    /**
     * Retira un problema de un concurso.
     *
     * @param contestProblemId Identificador del problema de concurso a retirar.
     */
    void execute(UUID contestProblemId);
}
