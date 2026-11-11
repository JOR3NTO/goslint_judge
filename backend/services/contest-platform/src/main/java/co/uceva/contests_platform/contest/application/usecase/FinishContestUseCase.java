package co.uceva.contests_platform.contest.application.usecase;

import java.util.UUID;

/**
 * Puerto de entrada para pasar un concurso a {@code ENDED} cuando llega su hora de fin.
 */
public interface FinishContestUseCase {
    /**
     * Ignora la llamada si el concurso no existe, ya está {@code ENDED}
     * o su hora de fin aún no llega (p. ej. tarea obsoleta tras reprogramar).
     *
     * @param contestId Identificador del concurso.
     */
    void execute(UUID contestId);
}
