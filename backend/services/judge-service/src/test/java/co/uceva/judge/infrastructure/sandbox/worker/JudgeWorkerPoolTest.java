package co.uceva.judge.infrastructure.sandbox.worker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import co.uceva.judge.domain.exception.SandboxExecutionException;
import co.uceva.judge.domain.model.JudgeResult;
import co.uceva.judge.domain.model.JudgeTask;
import co.uceva.judge.domain.model.TestCase;
import co.uceva.judge.infrastructure.sandbox.workspace.WorkerEnvironment;
import co.uceva.shared.domain.ProgrammingLanguage;
import co.uceva.shared.domain.VerdictStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class JudgeWorkerPoolTest {

    private static final long TIMEOUT_MS = 5_000;
    private static final long WAIT_SECONDS = 5;

    /** Entorno de cada worker, en orden: el del worker 1 es el primero. */
    private final List<WorkerEnvironment> environments = new ArrayList<>();
    private final ExecutorService callers = Executors.newCachedThreadPool();
    private JudgeWorkerPool pool;

    @AfterEach
    void tearDown() {
        if (pool != null) {
            pool.shutdown();
        }
        callers.shutdownNow();
    }

    /** Arranca un pool con {@code size} workers; los entornos que la prueba no haya añadido antes son simulados. */
    private void startPool(int size, long timeoutMillis, TaskEvaluator evaluator) {
        while (environments.size() < size) {
            environments.add(mock(WorkerEnvironment.class));
        }
        pool = new JudgeWorkerPool(environments, timeoutMillis, evaluator);
        pool.start();
    }

    private static JudgeTask task() {
        return JudgeTask.create(UUID.randomUUID(), ProgrammingLanguage.PYTHON, "print(1)",
                List.of(new TestCase(UUID.randomUUID(), "", "1", 0)), 1_000, 8_192);
    }

    private static JudgeResult result(JudgeTask task, VerdictStatus verdict) {
        return JudgeResult.create(task.getSubmissionId(), verdict, 1, 1, null);
    }

    private static JudgeResult accepted(JudgeTask task, WorkerEnvironment environment) {
        return result(task, VerdictStatus.ACCEPTED);
    }

    @Test
    void shouldHaveTheConfiguredNumberOfWorkersReadyOnStart() throws IOException {
        startPool(3, TIMEOUT_MS, JudgeWorkerPoolTest::accepted);

        assertThat(environments).hasSize(3);
        for (WorkerEnvironment environment : environments) {
            verify(environment).prepare();
        }
    }

    @Test
    void shouldEvaluateSubmissionsInParallelOnIndependentWorkers() throws Exception {
        // La barrera solo se abre si las dos evaluaciones están dentro a la vez.
        CyclicBarrier bothRunning = new CyclicBarrier(2);
        Set<WorkerEnvironment> usedEnvironments = ConcurrentHashMap.newKeySet();
        Set<String> usedThreads = ConcurrentHashMap.newKeySet();
        startPool(2, TIMEOUT_MS, (task, environment) -> {
            usedEnvironments.add(environment);
            usedThreads.add(Thread.currentThread().getName());
            await(bothRunning);
            return result(task, VerdictStatus.ACCEPTED);
        });

        Future<JudgeResult> first = callers.submit(() -> pool.execute(task()));
        Future<JudgeResult> second = callers.submit(() -> pool.execute(task()));

        assertThat(first.get(WAIT_SECONDS, TimeUnit.SECONDS).getVerdict()).isEqualTo(VerdictStatus.ACCEPTED);
        assertThat(second.get(WAIT_SECONDS, TimeUnit.SECONDS).getVerdict()).isEqualTo(VerdictStatus.ACCEPTED);
        assertThat(usedEnvironments).containsExactlyInAnyOrderElementsOf(environments);
        assertThat(usedThreads).containsExactlyInAnyOrder("judge-worker-1", "judge-worker-2");
    }

    @Test
    void shouldKeepSubmissionsWaitingWhileAllWorkersAreBusy() throws Exception {
        Semaphore started = new Semaphore(0);
        CountDownLatch finish = new CountDownLatch(1);
        startPool(1, TIMEOUT_MS, (task, environment) -> {
            started.release();
            await(finish);
            return result(task, VerdictStatus.ACCEPTED);
        });

        Future<JudgeResult> first = callers.submit(() -> pool.execute(task()));
        assertThat(started.tryAcquire(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
        Future<JudgeResult> second = callers.submit(() -> pool.execute(task()));

        assertThat(started.tryAcquire(300, TimeUnit.MILLISECONDS)).as("el segundo envío no empieza sin worker libre")
                .isFalse();
        finish.countDown();
        first.get(WAIT_SECONDS, TimeUnit.SECONDS);
        second.get(WAIT_SECONDS, TimeUnit.SECONDS);
        assertThat(started.tryAcquire()).as("el segundo envío se evalúa al liberarse el worker").isTrue();
    }

    @Test
    void shouldReleaseResourcesAndReuseTheWorkerAfterAnEvaluation() throws IOException {
        startPool(1, TIMEOUT_MS, JudgeWorkerPoolTest::accepted);

        pool.execute(task());
        pool.execute(task());

        WorkerEnvironment environment = environments.get(0);
        verify(environment, times(2)).clean();
        verify(environment, times(1)).prepare();
        verify(environment, never()).destroy();
    }

    @Test
    void shouldRestartAWorkerThatFailsAndKeepTheOthersWorking() throws Exception {
        JudgeTask poisoned = task();
        CyclicBarrier bothRunning = new CyclicBarrier(2);
        startPool(2, TIMEOUT_MS, (task, environment) -> {
            if (task == poisoned) {
                throw new IllegalStateException("fallo del worker");
            }
            await(bothRunning);
            return result(task, VerdictStatus.ACCEPTED);
        });

        assertThatThrownBy(() -> pool.execute(poisoned)).isInstanceOf(IllegalStateException.class)
                .hasMessage("fallo del worker");

        // El worker 1, que atendió el envío, recrea su entorno; el 2 no se entera.
        verify(environments.get(0), times(2)).prepare();
        verify(environments.get(1), times(1)).prepare();
        // El pool conserva su capacidad: siguen cabiendo dos evaluaciones a la vez.
        Future<JudgeResult> first = callers.submit(() -> pool.execute(task()));
        Future<JudgeResult> second = callers.submit(() -> pool.execute(task()));
        first.get(WAIT_SECONDS, TimeUnit.SECONDS);
        second.get(WAIT_SECONDS, TimeUnit.SECONDS);
    }

    @Test
    void shouldRestartAWorkerThatReportsAJudgeError() throws IOException {
        startPool(1, TIMEOUT_MS, (task, environment) -> result(task, VerdictStatus.JUDGE_ERROR));

        JudgeResult result = pool.execute(task());

        assertThat(result.getVerdict()).isEqualTo(VerdictStatus.JUDGE_ERROR);
        verify(environments.get(0), times(2)).prepare();
    }

    @Test
    void shouldReportAsSystemErrorAnEvaluationThatDiesWithAnError() throws IOException {
        startPool(1, TIMEOUT_MS, (task, environment) -> {
            throw new OutOfMemoryError("sin memoria");
        });

        assertThatThrownBy(() -> pool.execute(task())).isInstanceOf(SandboxExecutionException.class)
                .hasCauseInstanceOf(OutOfMemoryError.class);

        verify(environments.get(0), times(2)).prepare();
    }

    @Test
    void shouldGiveUpOnAHungWorkerAndRestartIt() throws Exception {
        JudgeTask hung = task();
        CountDownLatch interrupted = new CountDownLatch(1);
        startPool(1, 200, (task, environment) -> {
            if (task == hung) {
                try {
                    new CountDownLatch(1).await();
                } catch (InterruptedException e) {
                    interrupted.countDown();
                }
            }
            return result(task, VerdictStatus.ACCEPTED);
        });

        assertThatThrownBy(() -> pool.execute(hung)).isInstanceOf(SandboxExecutionException.class)
                .hasMessageContaining("tiempo máximo");

        assertThat(interrupted.await(WAIT_SECONDS, TimeUnit.SECONDS)).as("la evaluación colgada se interrumpe").isTrue();
        verify(environments.get(0), times(2)).prepare();
        assertThat(pool.execute(task()).getVerdict()).isEqualTo(VerdictStatus.ACCEPTED);
    }

    @Test
    void shouldRestartAWorkerThatCannotReleaseItsResources() throws IOException {
        WorkerEnvironment dirty = mock(WorkerEnvironment.class);
        doThrow(new IOException("hoja ocupada")).when(dirty).clean();
        environments.add(dirty);
        startPool(1, TIMEOUT_MS, JudgeWorkerPoolTest::accepted);

        JudgeResult result = pool.execute(task());

        assertThat(result.getVerdict()).isEqualTo(VerdictStatus.ACCEPTED);
        verify(dirty, times(2)).prepare();
    }

    @Test
    void shouldFailTheEvaluationWhileTheEnvironmentCannotBePreparedAndRecoverLater() throws IOException {
        WorkerEnvironment broken = mock(WorkerEnvironment.class);
        IOException noCgroups = new IOException("sin rama de cgroups");
        // Falla al arrancar el pool y al recibir el primer envío; después ya se puede crear.
        doThrow(noCgroups).doThrow(noCgroups).doNothing().when(broken).prepare();
        environments.add(broken);
        startPool(1, TIMEOUT_MS, JudgeWorkerPoolTest::accepted);

        assertThatThrownBy(() -> pool.execute(task())).isInstanceOf(SandboxExecutionException.class)
                .hasMessageContaining("entorno");

        assertThat(pool.execute(task()).getVerdict()).isEqualTo(VerdictStatus.ACCEPTED);
    }

    @Test
    void shouldDestroyItsEnvironmentsAndRejectSubmissionsAfterShutdown() throws IOException {
        startPool(2, TIMEOUT_MS, JudgeWorkerPoolTest::accepted);

        pool.shutdown();

        verify(environments.get(0)).destroy();
        verify(environments.get(1)).destroy();
        assertThatThrownBy(() -> pool.execute(task())).isInstanceOf(SandboxExecutionException.class)
                .hasMessageContaining("detenido");
    }

    @Test
    void shouldRejectAnInvalidConfiguration() {
        List<WorkerEnvironment> one = List.of(mock(WorkerEnvironment.class));

        assertThatThrownBy(() -> new JudgeWorkerPool(List.of(), TIMEOUT_MS, JudgeWorkerPoolTest::accepted))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new JudgeWorkerPool(one, 0, JudgeWorkerPoolTest::accepted))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static void await(CyclicBarrier barrier) {
        try {
            barrier.await(WAIT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IllegalStateException("las evaluaciones no coincidieron en el tiempo", e);
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(WAIT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }
}
