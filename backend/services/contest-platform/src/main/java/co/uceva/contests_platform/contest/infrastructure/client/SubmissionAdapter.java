package co.uceva.contests_platform.contest.infrastructure.client;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import co.uceva.contests_platform.contest.domain.model.TeamSubmission;
import co.uceva.contests_platform.submission.application.usecase.GetSubmissionsByTeamUseCase;
import co.uceva.shared.domain.VerdictStatus;
import org.springframework.stereotype.Component;

/**
 * Adaptador del módulo {@code contest} hacia el módulo {@code submission}: consulta los envíos
 * de un equipo llamando a su caso de uso, dentro del mismo proceso (sin HTTP).
 */
@Component
public class SubmissionAdapter {

    /** Rechazos que cuentan como intento fallido; compilación y fallos del juez no penalizan. */
    private static final Set<VerdictStatus> PENALIZED = EnumSet.of(VerdictStatus.WRONG_ANSWER,
            VerdictStatus.TIME_LIMIT_EXCEEDED, VerdictStatus.MEMORY_LIMIT_EXCEEDED, VerdictStatus.RUNTIME_ERROR);

    private final GetSubmissionsByTeamUseCase getSubmissionsByTeamUseCase;

    public SubmissionAdapter(GetSubmissionsByTeamUseCase getSubmissionsByTeamUseCase) {
        this.getSubmissionsByTeamUseCase = getSubmissionsByTeamUseCase;
    }

    /** Envíos del equipo con lo necesario para puntuarlos. */
    public List<TeamSubmission> findByTeamId(UUID teamId) {
        return getSubmissionsByTeamUseCase.execute(teamId).stream()
                .map(s -> new TeamSubmission(s.getProblemId(), s.getVerdict() == VerdictStatus.ACCEPTED,
                        PENALIZED.contains(s.getVerdict()), s.getSubmittedAt()))
                .toList();
    }
}
