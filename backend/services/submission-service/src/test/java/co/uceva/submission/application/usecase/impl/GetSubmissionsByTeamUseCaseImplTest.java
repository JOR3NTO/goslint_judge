package co.uceva.submission.application.usecase.impl;

import co.uceva.submission.domain.model.Submission;
import co.uceva.submission.domain.repository.SubmissionRepository;
import co.uceva.submission.fixtures.SubmissionFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetSubmissionsByTeamUseCaseImplTest {

    @Mock
    private SubmissionRepository submissionRepository;

    @InjectMocks
    private GetSubmissionsByTeamUseCaseImpl useCase;

    private final UUID teamId = UUID.randomUUID();

    @Test
    void shouldReturnSubmissionsByTeam() {
        List<Submission> submissions = List.of(
                SubmissionFixtures.aSubmission(UUID.randomUUID()),
                SubmissionFixtures.aSubmission(UUID.randomUUID())
        );
        when(submissionRepository.findByTeamId(teamId)).thenReturn(submissions);

        List<Submission> result = useCase.execute(teamId);

        assertThat(result).hasSize(2);
    }
}
