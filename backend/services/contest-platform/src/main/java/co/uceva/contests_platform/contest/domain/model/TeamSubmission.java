package co.uceva.contests_platform.contest.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Envío de un equipo visto desde el concurso: solo lo necesario para puntuarlo.
 *
 * @param problemId   Problema del concurso ({@link ContestProblem#getId()}) al que se envió.
 * @param accepted    {@code true} si el veredicto fue aceptado.
 * @param penalizes   {@code true} si es un rechazo que cuenta como intento fallido (WA, TLE, MLE, RTE).
 * @param submittedAt Momento del envío.
 */
public record TeamSubmission(UUID problemId, boolean accepted, boolean penalizes, Instant submittedAt) {
}
