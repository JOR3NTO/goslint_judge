package co.uceva.submission.domain.service;

import co.uceva.submission.domain.model.ContestSnapshot;
import co.uceva.submission.domain.model.SubmissionRejectionReason;
import co.uceva.submission.domain.model.TeamSnapshot;
import co.uceva.submission.domain.exception.SubmissionNotAllowedException;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Servicio de dominio que decide si un envío puede aceptarse.
 * <p>
 * Un envío solo se acepta si el usuario es integrante del equipo en cuyo nombre
 * envía, el concurso de ese equipo está activo y el problema pertenece a ese
 * concurso. La regla es la misma para todos los roles: un administrador u
 * organizador que quiera enviar también tiene que estar en un equipo del concurso.
 * </p>
 * <p>
 * El concurso no se recibe en la petición: se deduce del equipo, que pertenece a
 * un único concurso. Así el envío solo guarda su equipo y nunca puede quedar
 * asociado a un concurso distinto del que se validó.
 * </p>
 * <p>
 * Las reglas se evalúan en orden y la política consulta los datos a través de
 * {@link ContestDataSource} <em>solo cuando la regla siguiente los necesita</em>.
 * Así, el primer fallo corta la evaluación sin hacer llamadas innecesarias a
 * {@code contest-service} (por ejemplo, un no integrante nunca provoca la consulta
 * del concurso).
 * </p>
 */
public class SubmissionEligibilityPolicy {

    private final Clock clock;

    /**
     * @param clock Reloj con el que se determina si el concurso está activo; se
     *              inyecta para poder fijar el instante en las pruebas.
     */
    public SubmissionEligibilityPolicy(Clock clock) {
        this.clock = clock;
    }

    /**
     * Datos del intento de envío que la política necesita evaluar.
     *
     * @param submitterId Usuario autenticado que envía.
     * @param teamId      Equipo en cuyo nombre se envía.
     * @param problemId   Problema que se pretende resolver.
     */
    public record SubmissionAttempt(UUID submitterId, UUID teamId, UUID problemId) {}

    /**
     * Valida el envío.
     *
     * @param attempt Datos del intento de envío.
     * @param source  Fuente de datos de equipos y concursos.
     * @throws SubmissionNotAllowedException Si alguna regla no se cumple.
     */
    public void authorize(SubmissionAttempt attempt, ContestDataSource source) {
        TeamSnapshot team = source.findTeam(attempt.teamId())
                .orElseThrow(() -> reject(SubmissionRejectionReason.TEAM_NOT_FOUND));

        if (!team.hasMember(attempt.submitterId())) {
            throw reject(SubmissionRejectionReason.NOT_A_MEMBER);
        }

        ContestSnapshot contest = source.findContest(team.contestId())
                .orElseThrow(() -> reject(SubmissionRejectionReason.CONTEST_NOT_FOUND));

        Instant now = clock.instant();
        if (!contest.isActiveAt(now)) {
            throw reject(SubmissionRejectionReason.CONTEST_NOT_ACTIVE);
        }

        if (!source.findContestProblemIds(team.contestId()).contains(attempt.problemId())) {
            throw reject(SubmissionRejectionReason.PROBLEM_NOT_IN_CONTEST);
        }
    }

    private static SubmissionNotAllowedException reject(SubmissionRejectionReason reason) {
        return new SubmissionNotAllowedException(reason);
    }
}
