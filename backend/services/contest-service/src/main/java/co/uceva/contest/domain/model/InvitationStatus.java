package co.uceva.contest.domain.model;

/** Estado de una invitación a un equipo. */
public enum InvitationStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    /** Invalidada porque el equipo alcanzó el máximo de integrantes antes de aceptarla. */
    EXPIRED
}
