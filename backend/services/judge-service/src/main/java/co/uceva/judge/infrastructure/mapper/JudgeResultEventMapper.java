package co.uceva.judge.infrastructure.mapper;

import java.time.Instant;

import co.uceva.judge.domain.model.JudgeResult;
import co.uceva.shared.domain.event.SubmissionJudgedEvent;

/** Traduce el {@link JudgeResult} del dominio al evento compartido con {@code submission-service}. */
public final class JudgeResultEventMapper {

    private JudgeResultEventMapper() {}

    /**
     * @param result Resultado de la evaluación.
     * @return El evento que se publica hacia {@code submission-service}.
     */
    public static SubmissionJudgedEvent toEvent(JudgeResult result) {
        return new SubmissionJudgedEvent(result.getSubmissionId(), result.getVerdict(),
                result.getExecutionTimeMs(), result.getMemoryUsedKb(), Instant.now());
    }
}
