package co.uceva.submission.infrastructure.web.mapper;

import co.uceva.shared.domain.ProgrammingLanguage;
import co.uceva.shared.domain.VerdictStatus;
import co.uceva.submission.application.usecase.GetSubmissionMetricsUseCase.SubmissionMetrics;
import co.uceva.submission.application.usecase.SubmitCodeUseCase.SubmitCodeCommand;
import co.uceva.submission.domain.model.Submission;
import co.uceva.submission.fixtures.SubmissionFixtures;
import co.uceva.submission.infrastructure.web.dto.SubmissionMetricsResponseDTO;
import co.uceva.submission.infrastructure.web.dto.SubmissionResponseDTO;
import co.uceva.submission.infrastructure.web.dto.SubmitCodeRequestDTO;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SubmissionWebMapperTest {

    @Test
    void shouldMapRequestToCommand() {
        SubmitCodeRequestDTO dto = SubmissionFixtures.submitCodeRequest();

        SubmitCodeCommand command = SubmissionWebMapper.toCommand(dto);

        assertThat(command.teamId()).isEqualTo(dto.teamId());
        assertThat(command.problemId()).isEqualTo(dto.problemId());
        assertThat(command.language()).isEqualTo(dto.language());
        assertThat(command.sourceCode()).isEqualTo(dto.sourceCode());
    }

    @Test
    void shouldMapDomainToResponse() {
        Submission submission = SubmissionFixtures.aSubmission();

        SubmissionResponseDTO response = SubmissionWebMapper.toResponse(submission);

        assertThat(response.id()).isEqualTo(submission.getId());
        assertThat(response.teamId()).isEqualTo(submission.getTeamId());
        assertThat(response.problemId()).isEqualTo(submission.getProblemId());
        assertThat(response.language()).isEqualTo(submission.getLanguage());
        assertThat(response.sourceCode()).isEqualTo(submission.getSourceCode());
        assertThat(response.verdict()).isEqualTo(submission.getVerdict());
        assertThat(response.status()).isEqualTo(submission.getStatus());
        assertThat(response.executionTimeMs()).isEqualTo(submission.getExecutionTimeMs());
        assertThat(response.memoryUsedKb()).isEqualTo(submission.getMemoryUsedKb());
        assertThat(response.codeSizeBytes()).isEqualTo(submission.getCodeSizeBytes());
        assertThat(response.submittedAt()).isEqualTo(submission.getSubmittedAt());
    }

    @Test
    void shouldMapMetricsToResponse() {
        UUID id = UUID.randomUUID();
        SubmissionMetrics metrics = new SubmissionMetrics(id, VerdictStatus.ACCEPTED, 200, 8192, 1024);

        SubmissionMetricsResponseDTO response = SubmissionWebMapper.toResponse(metrics);

        assertThat(response.submissionId()).isEqualTo(id);
        assertThat(response.verdict()).isEqualTo(VerdictStatus.ACCEPTED);
        assertThat(response.executionTimeMs()).isEqualTo(200);
        assertThat(response.memoryUsedKb()).isEqualTo(8192);
        assertThat(response.codeSizeBytes()).isEqualTo(1024);
    }
}
