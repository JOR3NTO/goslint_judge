package co.uceva.contests_platform.contest.application.usecase;

import java.util.UUID;

/**
 * Puerto de entrada para congelar la tabla pública de un concurso cuando llega su hora de congelamiento.
 */
public interface FreezeScoreboardUseCase {
    /**
     * Ignora la llamada si el concurso no existe, no se congela, su tabla ya no está en vivo
     * o la hora de congelamiento aún no llega (p. ej. tarea obsoleta tras reprogramar).
     *
     * @param contestId Identificador del concurso.
     */
    void execute(UUID contestId);
}
