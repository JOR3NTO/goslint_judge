package co.uceva.judge.infrastructure.sandbox;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.uceva.judge.domain.exception.SandboxExecutionException;
import co.uceva.judge.domain.model.JudgeResult;
import co.uceva.judge.domain.model.JudgeTask;
import co.uceva.judge.domain.model.TestCase;
import co.uceva.shared.domain.ProgrammingLanguage;
import co.uceva.shared.domain.VerdictStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RunnerSandboxExecutorTest {

    @Mock private Runner runner;

    private JudgeTask task(ProgrammingLanguage language) {
        return JudgeTask.create(UUID.randomUUID(), language, "print(1)",
                List.of(new TestCase(UUID.randomUUID(), "", "1", 0)), 1_000, 8_192);
    }

    @Test
    void shouldRunSolutionAndMapAcceptedResult() {
        JudgeTask task = task(ProgrammingLanguage.PYTHON);
        Map<String, Object> runnerResult = new HashMap<>();
        runnerResult.put("status", VerdictStatus.ACCEPTED);
        runnerResult.put("maxCpuTime", 15L);
        runnerResult.put("maxMemoryUsed", 2_048L);
        Path[] seen = new Path[1];
        doAnswer(invocation -> {
            seen[0] = Path.of((String) invocation.getArgument(1));
            assertThat(Files.readString(seen[0])).isEqualTo("print(1)");
            return runnerResult;
        }).when(runner).runSolution(eq("python3"), any(String.class), eq(task.getTestCases()), eq(1_000L),
                eq(8_192L * 1024));

        JudgeResult result = new RunnerSandboxExecutor(runner).execute(task);

        assertThat(result.getVerdict()).isEqualTo(VerdictStatus.ACCEPTED);
        assertThat(result.getExecutionTimeMs()).isEqualTo(15);
        assertThat(result.getMemoryUsedKb()).isEqualTo(2_048);
        assertThat(result.getFailedTestCase()).isNull();
        assertThat(seen[0]).doesNotExist();
    }

    @Test
    void shouldMapFailedTestCase() {
        JudgeTask task = task(ProgrammingLanguage.PYTHON);
        UUID failed = task.getTestCases().get(0).id();
        Map<String, Object> runnerResult = new HashMap<>();
        runnerResult.put("status", VerdictStatus.WRONG_ANSWER);
        runnerResult.put("maxCpuTime", 5L);
        runnerResult.put("maxMemoryUsed", 100L);
        runnerResult.put("failedTestCase", failed);
        when(runner.runSolution(any(), any(), any(), anyLong(), anyLong())).thenReturn(runnerResult);

        JudgeResult result = new RunnerSandboxExecutor(runner).execute(task);

        assertThat(result.getVerdict()).isEqualTo(VerdictStatus.WRONG_ANSWER);
        assertThat(result.getFailedTestCase()).isEqualTo(failed);
    }

    @Test
    void shouldFailAsSystemErrorForUnsupportedLanguage() {
        assertThatThrownBy(() -> new RunnerSandboxExecutor(runner).execute(task(ProgrammingLanguage.JAVA)))
                .isInstanceOf(SandboxExecutionException.class)
                .hasMessageContaining("JAVA");
        verifyNoInteractions(runner);
    }

    @Test
    void shouldFailWhenRunnerResultIsIncomplete() {
        when(runner.runSolution(any(), any(), any(), anyLong(), anyLong())).thenReturn(new HashMap<>());

        assertThatThrownBy(() -> new RunnerSandboxExecutor(runner).execute(task(ProgrammingLanguage.PYTHON)))
                .isInstanceOf(SandboxExecutionException.class);
    }
}
