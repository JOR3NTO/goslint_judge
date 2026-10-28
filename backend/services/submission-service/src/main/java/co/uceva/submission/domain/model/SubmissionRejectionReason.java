package co.uceva.submission.domain.model;

/**
 * Motivos por los que un envío no cumple las reglas de elegibilidad.
 */
public enum SubmissionRejectionReason {
    /** El equipo indicado no existe. */
    TEAM_NOT_FOUND,
    /** El usuario no es integrante del equipo indicado. */
    NOT_A_MEMBER,
    /** El concurso no existe. */
    CONTEST_NOT_FOUND,
    /** El concurso no está en curso en este momento. */
    CONTEST_NOT_ACTIVE,
    /** El problema no forma parte del concurso. */
    PROBLEM_NOT_IN_CONTEST
}
