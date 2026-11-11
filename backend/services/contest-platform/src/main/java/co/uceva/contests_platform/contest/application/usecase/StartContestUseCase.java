package co.uceva.contests_platform.contest.application.usecase;

import java.util.UUID;

/**
 * Puerto de entrada para pasar un concurso a {@code ACTIVE} cuando llega su hora de inicio.
 */
public interface StartContestUseCase {
    /**
     * Ignora la llamada si el concurso no existe, ya no está {@code UPCOMING}
     * o su hora de inicio aún no llega (p. ej. tarea obsoleta tras reprogramar).
     *
     * @param contestId Identificador del concurso.
     */
    void execute(UUID contestId);
}
