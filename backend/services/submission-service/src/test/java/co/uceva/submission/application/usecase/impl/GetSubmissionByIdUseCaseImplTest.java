package co.uceva.submission.application.usecase.impl;

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
class GetSubmissionByIdUseCaseImplTest {

    @Mock
    private SubmissionRepository submissionRepository;

    @InjectMocks
    private GetSubmissionByIdUseCaseImpl useCase;

    private final UUID submissionId = UUID.randomUUID();

    @Test
    void shouldReturnSubmissionWhenFound() {
        Submission submission = SubmissionFixtures.aSubmission(submissionId);
        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));

        Submission result = useCase.execute(submissionId);

        assertThat(result).isEqualTo(submission);
    }

    @Test
    void shouldThrowWhenNotFound() {
        when(submissionRepository.findById(submissionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(submissionId))
                .isInstanceOf(SubmissionNotFoundException.class);
    }
}
