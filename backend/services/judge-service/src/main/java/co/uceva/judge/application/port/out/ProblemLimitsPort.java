package co.uceva.judge.application.port.out;

import java.util.UUID;

/**
 * Puerto de salida para obtener los límites de ejecución de un problema, que
 * pertenecen a {@code problem-service}.
 */
public interface ProblemLimitsPort {

    /**
     * Recupera los límites de tiempo y memoria de un problema.
     *
     * @param problemId Identificador del problema.
     * @return Los límites configurados para el problema.
     */
    ProblemLimits findByProblemId(UUID problemId);
}
