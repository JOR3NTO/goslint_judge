package co.uceva.submission.infrastructure.messaging;

import co.uceva.submission.application.usecase.EnqueueSubmissionUseCase;
import co.uceva.submission.domain.model.Submission;
import co.uceva.submission.domain.repository.SubmissionRepository;
import co.uceva.submission.fixtures.SubmissionFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PendingSubmissionRetrySchedulerTest {

    private static final long GRACE_PERIOD_MS = 15_000L;
    private static final int BATCH_SIZE = 50;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private EnqueueSubmissionUseCase enqueueSubmissionUseCase;

    private PendingSubmissionRetryScheduler scheduler() {
        return new PendingSubmissionRetryScheduler(
                submissionRepository, enqueueSubmissionUseCase, GRACE_PERIOD_MS, BATCH_SIZE);
    }

    @Test
    void shouldRetryEveryStalePendingSubmission() {
        Submission first = SubmissionFixtures.aSubmission(UUID.randomUUID());
        Submission second = SubmissionFixtures.aSubmission(UUID.randomUUID());
        when(submissionRepository.findStalePending(any(Instant.class), anyInt()))
                .thenReturn(List.of(first, second));

        scheduler().reintentarEnviosPendientes();

        verify(enqueueSubmissionUseCase).execute(first);
        verify(enqueueSubmissionUseCase).execute(second);
    }

    @Test
    void shouldOnlyConsiderSubmissionsOlderThanTheGracePeriod() {
        when(submissionRepository.findStalePending(any(Instant.class), anyInt())).thenReturn(List.of());
        Instant antesDeLlamar = Instant.now();

        scheduler().reintentarEnviosPendientes();

        Instant despuesDeLlamar = Instant.now();
        ArgumentCaptor<Instant> threshold = ArgumentCaptor.forClass(Instant.class);
        verify(submissionRepository).findStalePending(threshold.capture(), anyInt());

        // El umbral es "ahora menos el periodo de gracia", lo que deja fuera los
        // envíos recientes que el flujo normal todavía está entregando.
        assertThat(threshold.getValue())
                .isBetween(antesDeLlamar.minusMillis(GRACE_PERIOD_MS),
                        despuesDeLlamar.minusMillis(GRACE_PERIOD_MS));
    }

    @Test
    void shouldLimitTheBatchSize() {
        when(submissionRepository.findStalePending(any(Instant.class), anyInt())).thenReturn(List.of());

        scheduler().reintentarEnviosPendientes();

        verify(submissionRepository).findStalePending(any(Instant.class), org.mockito.ArgumentMatchers.eq(BATCH_SIZE));
    }

    @Test
    void shouldDoNothingWhenThereIsNothingPending() {
        when(submissionRepository.findStalePending(any(Instant.class), anyInt())).thenReturn(List.of());

        scheduler().reintentarEnviosPendientes();

        verify(enqueueSubmissionUseCase, never()).execute(any());
    }
}
