package co.uceva.judge.infrastructure.mapper;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import co.uceva.judge.domain.model.JudgeResult;
import co.uceva.shared.domain.VerdictStatus;
import co.uceva.shared.domain.event.SubmissionJudgedEvent;

import static org.assertj.core.api.Assertions.assertThat;

class JudgeResultEventMapperTest {

    @Test
    void shouldMapResultToEvent() {
        UUID id = UUID.randomUUID();
        JudgeResult result = JudgeResult.create(id, VerdictStatus.WRONG_ANSWER, 120, 2_048, UUID.randomUUID());

        SubmissionJudgedEvent event = JudgeResultEventMapper.toEvent(result);

        assertThat(event.submissionId()).isEqualTo(id);
        assertThat(event.verdict()).isEqualTo(VerdictStatus.WRONG_ANSWER);
        assertThat(event.executionTimeMs()).isEqualTo(120);
        assertThat(event.memoryUsedKb()).isEqualTo(2_048);
        assertThat(event.judgedAt()).isNotNull();
    }
}
