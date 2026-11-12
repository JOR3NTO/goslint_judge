package co.uceva.contests_platform.contest.infrastructure.client;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import co.uceva.contests_platform.submission.application.usecase.GetSubmissionsByTeamUseCase;
import co.uceva.contests_platform.submission.domain.model.Submission;
import co.uceva.shared.domain.VerdictStatus;
import org.springframework.stereotype.Component;

/**
 * Adaptador del módulo {@code contest} hacia el módulo {@code submission}: consulta los envíos
 * de un equipo llamando a su caso de uso, dentro del mismo proceso (sin HTTP).
 */
@Component
public class SubmissionAdapter {

    private final GetSubmissionsByTeamUseCase getSubmissionsByTeamUseCase;

    public SubmissionAdapter(GetSubmissionsByTeamUseCase getSubmissionsByTeamUseCase) {
        this.getSubmissionsByTeamUseCase = getSubmissionsByTeamUseCase;
    }

    /**
     * Momento en que el equipo resolvió su último problema: de cada problema solo cuenta el primer
     * envío aceptado, y de esos se toma el más reciente. Vacío si el equipo no ha resuelto ninguno.
     */
    public Optional<Instant> findLastSolvedAt(UUID teamId) {
        Map<UUID, Instant> firstAcceptedByProblem = new HashMap<>();
        getSubmissionsByTeamUseCase.execute(teamId).stream()
                .filter(s -> s.getVerdict() == VerdictStatus.ACCEPTED)
                .sorted(Comparator.comparing(Submission::getSubmittedAt))
                .forEach(s -> firstAcceptedByProblem.putIfAbsent(s.getProblemId(), s.getSubmittedAt()));
        return firstAcceptedByProblem.values().stream().max(Comparator.naturalOrder());
    }
}
