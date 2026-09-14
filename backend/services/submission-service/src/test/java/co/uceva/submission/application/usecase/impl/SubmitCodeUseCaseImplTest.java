package co.uceva.submission.application.usecase.impl;

import co.uceva.shared.domain.SubmissionStatus;
import co.uceva.submission.application.event.SubmissionPersistedEvent;
import co.uceva.submission.application.usecase.SubmitCodeUseCase.SubmitCodeCommand;
import co.uceva.submission.domain.exception.DuplicateSubmissionException;
import co.uceva.submission.domain.model.Submission;
import co.uceva.submission.domain.repository.SubmissionRepository;
import co.uceva.submission.fixtures.SubmissionFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmitCodeUseCaseImplTest {

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private SubmitCodeUseCaseImpl useCase;

    @Test
    void shouldCreateSubmissionAndSignalItIsReadyToEnqueue() {
        SubmitCodeCommand command = SubmissionFixtures.submitCodeCommand();
        when(submissionRepository.existsByTeamIdAndProblemIdAndSourceCodeAndLanguage(
                command.teamId(), command.problemId(), command.sourceCode(), command.language())).thenReturn(false);
        when(submissionRepository.save(any(Submission.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Submission result = useCase.execute(command);

        assertThat(result).isNotNull();
        assertThat(result.getTeamId()).isEqualTo(command.teamId());
        assertThat(result.getProblemId()).isEqualTo(command.problemId());
        assertThat(result.getVerdict().name()).isEqualTo("PENDING");
        verify(submissionRepository).save(any(Submission.class));
        verify(applicationEventPublisher).publishEvent(new SubmissionPersistedEvent(result));
    }

    @Test
    void shouldLeaveNewSubmissionPendingUntilItIsActuallyEnqueued() {
        SubmitCodeCommand command = SubmissionFixtures.submitCodeCommand();
        when(submissionRepository.existsByTeamIdAndProblemIdAndSourceCodeAndLanguage(
                command.teamId(), command.problemId(), command.sourceCode(), command.language())).thenReturn(false);
        when(submissionRepository.save(any(Submission.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Submission result = useCase.execute(command);

        assertThat(result.getStatus()).isEqualTo(SubmissionStatus.PENDING);
    }

    @Test
    void shouldThrowWhenDuplicateExists() {
        SubmitCodeCommand command = SubmissionFixtures.submitCodeCommand();
        when(submissionRepository.existsByTeamIdAndProblemIdAndSourceCodeAndLanguage(
                command.teamId(), command.problemId(), command.sourceCode(), command.language())).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(DuplicateSubmissionException.class);

        verify(submissionRepository, never()).save(any());
        verify(applicationEventPublisher, never()).publishEvent(any(SubmissionPersistedEvent.class));
    }
}
