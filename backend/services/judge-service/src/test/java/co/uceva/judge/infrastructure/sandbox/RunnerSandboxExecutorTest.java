package co.uceva.judge.infrastructure.sandbox;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.uceva.judge.domain.exception.SandboxExecutionException;
import co.uceva.judge.domain.model.JudgeResult;
import co.uceva.judge.domain.model.JudgeTask;
import co.uceva.judge.domain.model.TestCase;
import co.uceva.judge.infrastructure.sandbox.workspace.WorkerEnvironment;
import co.uceva.shared.domain.ProgrammingLanguage;
import co.uceva.shared.domain.VerdictStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * El {@code Compiler} es estático y lanza el compilador real, así que estas
 * pruebas usan Python y se saltan en una máquina sin {@code python3}.
 */
@ExtendWith(MockitoExtension.class)
class RunnerSandboxExecutorTest {

    @Mock private Runner runner;
    @TempDir Path tempDir;

    private WorkerEnvironment environment;

    @BeforeEach
    void setUp() throws IOException {
        assumeTrue(python3IsAvailable(), "python3 no está instalado");
        Path workDir = Files.createDirectory(tempDir.resolve("work"));
        environment = new WorkerEnvironment(tempDir.resolve("cg"), workDir);
    }

    private static boolean python3IsAvailable() {
        try {
            return new ProcessBuilder("python3", "--version").redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD).start().waitFor() == 0;
        } catch (IOException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private JudgeTask task(String sourceCode) {
        return JudgeTask.create(UUID.randomUUID(), ProgrammingLanguage.PYTHON, sourceCode,
                List.of(new TestCase(UUID.randomUUID(), "", "1", 0)), 1_000, 8_192);
    }

    @Test
    void shouldRunSolutionInsideTheWorkerEnvironmentAndMapAcceptedResult() {
        JudgeTask task = task("print(1)");
        Map<String, Object> runnerResult = new HashMap<>();
        runnerResult.put("status", VerdictStatus.ACCEPTED);
        runnerResult.put("maxCpuTime", 15L);
        runnerResult.put("maxMemoryUsed", 2_048L);
        Path[] seen = new Path[1];
        doAnswer(invocation -> {
            seen[0] = Path.of((String) invocation.getArgument(2));
            assertThat(Files.readString(seen[0])).isEqualTo("print(1)");
            return runnerResult;
        }).when(runner).runSolution(eq(environment), any(), any(String.class), eq(task.getTestCases()), eq(1_000L),
                eq(8_192L * 1024));

        JudgeResult result = new RunnerSandboxExecutor(runner).evaluate(task, environment);

        assertThat(result.getVerdict()).isEqualTo(VerdictStatus.ACCEPTED);
        assertThat(result.getExecutionTimeMs()).isEqualTo(15);
        assertThat(result.getMemoryUsedKb()).isEqualTo(2_048);
        assertThat(result.getFailedTestCase()).isNull();
        assertThat(seen[0]).startsWithRaw(environment.workDir());
        assertThat(environment.workDir()).isEmptyDirectory();
    }

    @Test
    void shouldMapFailedTestCase() {
        JudgeTask task = task("print(1)");
        UUID failed = task.getTestCases().get(0).id();
        Map<String, Object> runnerResult = new HashMap<>();
        runnerResult.put("status", VerdictStatus.WRONG_ANSWER);
        runnerResult.put("maxCpuTime", 5L);
        runnerResult.put("maxMemoryUsed", 100L);
        runnerResult.put("failedTestCase", failed);
        when(runner.runSolution(any(), any(), any(), any(), anyLong(), anyLong())).thenReturn(runnerResult);

        JudgeResult result = new RunnerSandboxExecutor(runner).evaluate(task, environment);

        assertThat(result.getVerdict()).isEqualTo(VerdictStatus.WRONG_ANSWER);
        assertThat(result.getFailedTestCase()).isEqualTo(failed);
    }

    @Test
    void shouldReturnCompilationErrorWithoutRunningTheSolution() {
        JudgeResult result = new RunnerSandboxExecutor(runner).evaluate(task("print("), environment);

        assertThat(result.getVerdict()).isEqualTo(VerdictStatus.COMPILATION_ERROR);
        verifyNoInteractions(runner);
        assertThat(environment.workDir()).isEmptyDirectory();
    }

    @Test
    void shouldFailWhenRunnerResultIsIncomplete() {
        when(runner.runSolution(any(), any(), any(), any(), anyLong(), anyLong())).thenReturn(new HashMap<>());

        assertThatThrownBy(() -> new RunnerSandboxExecutor(runner).evaluate(task("print(1)"), environment))
                .isInstanceOf(SandboxExecutionException.class);
    }

    @Test
    void shouldFailAsSystemErrorWhenTheWorkerDirectoryIsMissing() {
        WorkerEnvironment missing = new WorkerEnvironment(tempDir.resolve("cg"), tempDir.resolve("no-existe"));

        assertThatThrownBy(() -> new RunnerSandboxExecutor(runner).evaluate(task("print(1)"), missing))
                .isInstanceOf(SandboxExecutionException.class);
        verifyNoInteractions(runner);
    }
}
