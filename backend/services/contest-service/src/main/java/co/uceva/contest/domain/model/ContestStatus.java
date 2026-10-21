package co.uceva.contest.domain.model;

/**
 * Enumeración que representa el estado del ciclo de vida de un concurso (maratón)
 * de programación dentro de la plataforma.
 */
public enum ContestStatus {
    /** El concurso está programado y aún no ha comenzado. */
    UPCOMING,
    /** El concurso está actualmente en curso. */
    ACTIVE,
    /** El concurso ha finalizado. */
    ENDED
}

