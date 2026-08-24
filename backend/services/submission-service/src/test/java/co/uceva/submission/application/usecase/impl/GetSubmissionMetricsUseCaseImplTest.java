package co.uceva.submission.application.usecase.impl;

import co.uceva.shared.domain.VerdictStatus;
import co.uceva.submission.application.usecase.GetSubmissionMetricsUseCase.SubmissionMetrics;
import co.uceva.submission.domain.exception.SubmissionNotFoundException;
import co.uceva.submission.domain.model.Submission;
import co.uceva.submission.domain.repository.SubmissionRepository;
import co.uceva.submission.fixtures.SubmissionFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetSubmissionMetricsUseCaseImplTest {

    @Mock
    private SubmissionRepository submissionRepository;

    @InjectMocks
    private GetSubmissionMetricsUseCaseImpl useCase;

    private final UUID submissionId = UUID.randomUUID();

    @Test
    void shouldReturnMetricsWhenFound() {
        Submission submission = SubmissionFixtures.aSubmission(submissionId, VerdictStatus.ACCEPTED);
        submission.updateVerdict(VerdictStatus.ACCEPTED, 150, 4096);
        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));

        SubmissionMetrics result = useCase.execute(submissionId);

        assertThat(result.submissionId()).isEqualTo(submissionId);
        assertThat(result.verdict()).isEqualTo(VerdictStatus.ACCEPTED);
        assertThat(result.executionTimeMs()).isEqualTo(150);
        assertThat(result.memoryUsedKb()).isEqualTo(4096);
        assertThat(result.codeSizeBytes()).isEqualTo(submission.getCodeSizeBytes());
    }

    @Test
    void shouldThrowWhenNotFound() {
        when(submissionRepository.findById(submissionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(submissionId))
                .isInstanceOf(SubmissionNotFoundException.class);
    }
}
