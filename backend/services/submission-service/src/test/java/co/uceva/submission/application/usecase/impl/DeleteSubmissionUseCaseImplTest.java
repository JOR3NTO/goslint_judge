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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteSubmissionUseCaseImplTest {

    @Mock
    private SubmissionRepository submissionRepository;

    @InjectMocks
    private DeleteSubmissionUseCaseImpl useCase;

    private final UUID submissionId = UUID.randomUUID();

    @Test
    void shouldDeleteWhenExists() {
        Submission submission = SubmissionFixtures.aSubmission(submissionId);
        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));

        useCase.execute(submissionId);

        verify(submissionRepository).deleteById(submissionId);
    }

    @Test
    void shouldThrowWhenNotFound() {
        when(submissionRepository.findById(submissionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(submissionId))
                .isInstanceOf(SubmissionNotFoundException.class);
    }
}
