package co.uceva.judge.application.usecase.impl;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.uceva.judge.application.exception.JudgingNotificationException;
import co.uceva.judge.application.port.out.JudgeResultPublisher;
import co.uceva.judge.application.port.out.ProblemLimits;
import co.uceva.judge.application.port.out.ProblemLimitsPort;
import co.uceva.judge.application.port.out.SandboxExecutor;
import co.uceva.judge.application.port.out.SubmissionJudgingNotifier;
import co.uceva.judge.domain.exception.SandboxExecutionException;
import co.uceva.judge.domain.exception.TestCasesNotFoundException;
import co.uceva.judge.domain.model.JudgeResult;
import co.uceva.judge.domain.model.JudgeTask;
import co.uceva.judge.domain.model.TestCase;
import co.uceva.judge.domain.repository.TestCaseRepository;
import co.uceva.judge.domain.valueobject.MemoryLimit;
import co.uceva.judge.domain.valueobject.TimeLimit;
import co.uceva.shared.domain.ProgrammingLanguage;
import co.uceva.shared.domain.VerdictStatus;
import co.uceva.shared.domain.event.SubmissionReceivedEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvaluateSubmissionUseCaseImplTest {

    @Mock private TestCaseRepository testCaseRepository;
    @Mock private ProblemLimitsPort problemLimitsPort;
    @Mock private SandboxExecutor sandboxExecutor;
    @Mock private JudgeResultPublisher judgeResultPublisher;
    @Mock private SubmissionJudgingNotifier submissionJudgingNotifier;
    @InjectMocks private EvaluateSubmissionUseCaseImpl useCase;

    private SubmissionReceivedEvent event;
    private List<TestCase> testCases;

    @BeforeEach
    void setUp() {
        event = new SubmissionReceivedEvent(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                ProgrammingLanguage.PYTHON, "print(1)", Instant.now());
        testCases = List.of(new TestCase(UUID.randomUUID(), "", "1", 0));
    }

    @Test
    void shouldNotifyJudgingStartedBeforeEvaluatingAndPublishResult() {
        JudgeResult expected = JudgeResult.create(event.submissionId(), VerdictStatus.ACCEPTED, 10, 100, null);
        when(testCaseRepository.findAllByProblemId(event.problemId())).thenReturn(testCases);
        when(problemLimitsPort.findByProblemId(event.problemId()))
                .thenReturn(new ProblemLimits(new TimeLimit(1_000), new MemoryLimit(8_192)));
        when(sandboxExecutor.execute(any(JudgeTask.class))).thenReturn(expected);

        JudgeResult result = useCase.execute(event);

        ArgumentCaptor<JudgeTask> captor = ArgumentCaptor.forClass(JudgeTask.class);
        verify(sandboxExecutor).execute(captor.capture());
        assertThat(captor.getValue().getSubmissionId()).isEqualTo(event.submissionId());
        assertThat(captor.getValue().getTimeLimit().milliseconds()).isEqualTo(1_000);
        assertThat(captor.getValue().getMemoryLimit().kilobytes()).isEqualTo(8_192);
        assertThat(captor.getValue().getTestCases()).isEqualTo(testCases);
        verify(judgeResultPublisher).publish(expected);
        assertThat(result).isSameAs(expected);

        InOrder order = inOrder(submissionJudgingNotifier, sandboxExecutor, judgeResultPublisher);
        order.verify(submissionJudgingNotifier).notifyJudgingStarted(event.submissionId());
        order.verify(sandboxExecutor).execute(any(JudgeTask.class));
        order.verify(judgeResultPublisher).publish(expected);
    }

    @Test
    void shouldPropagateJudgingNotificationFailureWithoutEvaluating() {
        doThrow(new JudgingNotificationException(event.submissionId(), "el broker no acusó recibo"))
                .when(submissionJudgingNotifier).notifyJudgingStarted(event.submissionId());

        assertThatThrownBy(() -> useCase.execute(event)).isInstanceOf(JudgingNotificationException.class);
        verifyNoInteractions(testCaseRepository, problemLimitsPort, sandboxExecutor, judgeResultPublisher);
    }

    @Test
    void shouldPropagateMissingTestCasesWithoutPublishing() {
        when(testCaseRepository.findAllByProblemId(event.problemId()))
                .thenThrow(new TestCasesNotFoundException(event.problemId()));

        assertThatThrownBy(() -> useCase.execute(event)).isInstanceOf(TestCasesNotFoundException.class);
        verify(submissionJudgingNotifier).notifyJudgingStarted(event.submissionId());
        verifyNoInteractions(sandboxExecutor, judgeResultPublisher);
    }

    @Test
    void shouldPropagateSandboxFailureWithoutPublishing() {
        when(testCaseRepository.findAllByProblemId(event.problemId())).thenReturn(testCases);
        when(problemLimitsPort.findByProblemId(event.problemId()))
                .thenReturn(new ProblemLimits(new TimeLimit(1_000), new MemoryLimit(8_192)));
        when(sandboxExecutor.execute(any(JudgeTask.class)))
                .thenThrow(new SandboxExecutionException("fallo", new RuntimeException()));

        assertThatThrownBy(() -> useCase.execute(event)).isInstanceOf(SandboxExecutionException.class);
        verifyNoInteractions(judgeResultPublisher);
    }
}
