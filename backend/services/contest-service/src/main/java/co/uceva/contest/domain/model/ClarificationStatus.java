package co.uceva.contest.domain.model;

/**
 * Enumeración que representa el estado de una aclaración (pregunta de un equipo
 * sobre un problema del concurso).
 */
public enum ClarificationStatus {
    /** La pregunta fue enviada y aún no tiene respuesta. */
    PENDING,
    /** Un organizador ya respondió la pregunta. */
    ANSWERED
}
