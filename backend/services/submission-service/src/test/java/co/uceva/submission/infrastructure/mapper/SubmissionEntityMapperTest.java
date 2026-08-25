package co.uceva.submission.infrastructure.mapper;

import co.uceva.shared.domain.ProgrammingLanguage;
import co.uceva.shared.domain.SubmissionStatus;
import co.uceva.shared.domain.VerdictStatus;
import co.uceva.submission.domain.model.Submission;
import co.uceva.submission.infrastructure.persistence.entity.SubmissionEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SubmissionEntityMapperTest {

    @Test
    void shouldMapDomainToEntityAndBack() {
        Submission domain = Submission.builder()
                .id(UUID.randomUUID())
                .teamId(UUID.randomUUID())
                .problemId(UUID.randomUUID())
                .language(ProgrammingLanguage.JAVA)
                .sourceCode("class Main {}")
                .verdict(VerdictStatus.PENDING)
                .status(SubmissionStatus.QUEUED)
                .executionTimeMs(0)
                .memoryUsedKb(0)
                .submittedAt(Instant.now())
                .build();

        SubmissionEntity entity = SubmissionEntityMapper.toEntity(domain);
        Submission mapped = SubmissionEntityMapper.toDomain(entity);

        assertThat(mapped.getId()).isEqualTo(domain.getId());
        assertThat(mapped.getTeamId()).isEqualTo(domain.getTeamId());
        assertThat(mapped.getProblemId()).isEqualTo(domain.getProblemId());
        assertThat(mapped.getLanguage()).isEqualTo(domain.getLanguage());
        assertThat(mapped.getSourceCode()).isEqualTo(domain.getSourceCode());
        assertThat(mapped.getVerdict()).isEqualTo(domain.getVerdict());
        assertThat(mapped.getStatus()).isEqualTo(domain.getStatus());
        assertThat(entity.getStatus()).isEqualTo(SubmissionStatus.QUEUED);
        assertThat(mapped.getExecutionTimeMs()).isEqualTo(domain.getExecutionTimeMs());
        assertThat(mapped.getMemoryUsedKb()).isEqualTo(domain.getMemoryUsedKb());
        assertThat(mapped.getCodeSizeBytes()).isEqualTo(domain.getCodeSizeBytes());
        assertThat(mapped.getSubmittedAt()).isEqualTo(domain.getSubmittedAt());
    }

    @Test
    void shouldReturnNullForNullInput() {
        assertThat(SubmissionEntityMapper.toEntity(null)).isNull();
        assertThat(SubmissionEntityMapper.toDomain(null)).isNull();
    }
}
