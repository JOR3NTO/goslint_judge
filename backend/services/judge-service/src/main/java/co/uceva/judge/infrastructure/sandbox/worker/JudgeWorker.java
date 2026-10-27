package co.uceva.judge.infrastructure.sandbox.worker;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.uceva.judge.domain.exception.SandboxExecutionException;
import co.uceva.judge.domain.model.JudgeResult;
import co.uceva.judge.domain.model.JudgeTask;
import co.uceva.judge.infrastructure.sandbox.workspace.WorkerEnvironment;

/**
 * Un worker de evaluación: un puesto con su propio {@link WorkerEnvironment} que
 * atiende un envío cada vez, en un hilo aparte llamado {@code judge-worker-<id>}.
 * <p>
 * El {@link JudgeWorkerPool} lo marca como ocupado antes de usarlo y no lo
 * entrega a nadie más hasta que queda libre, así que sus métodos nunca se
 * llaman desde dos hilos a la vez.
 * </p>
 */
final class JudgeWorker {

    private static final Logger log = LoggerFactory.getLogger(JudgeWorker.class);

    /** Tiempo que se espera a que termine el hilo de una evaluación abandonada. */
    private static final long ABANDON_WAIT_MS = 5_000;

    private final int id;
    private final WorkerEnvironment environment;
    /** {@code true} mientras atiende un envío. Solo lo lee y lo cambia el pool. */
    private boolean busy;
    /** {@code true} si el entorno está creado y se puede evaluar en él. */
    private boolean prepared;

    JudgeWorker(int id, WorkerEnvironment environment) {
        this.id = id;
        this.environment = environment;
    }

    /**
     * Crea el entorno del worker desde cero, eliminando lo que hubiera. Sirve
     * tanto para arrancarlo como para reiniciarlo tras un fallo. Si no se
     * puede, se registra y se reintenta al recibir el siguiente envío.
     *
     * @return {@code true} si el entorno quedó listo.
     */
    boolean prepare() {
        try {
            environment.prepare();
            prepared = true;
        } catch (IOException e) {
            prepared = false;
            log.error("No se pudo preparar el entorno del worker {}: {}", id, e.toString());
        }
        return prepared;
    }

    /**
     * Evalúa la tarea en un hilo aparte y espera a que termine, como mucho
     * {@code timeoutMillis}.
     *
     * @param task          Tarea de evaluación.
     * @param evaluator     Evaluación a ejecutar dentro del entorno del worker.
     * @param timeoutMillis Tiempo máximo de espera; superado, el worker se da por colgado.
     * @return El resultado de la evaluación.
     * @throws SandboxExecutionException Si el entorno no está disponible, la evaluación
     *                                   supera el tiempo máximo o es interrumpida.
     */
    JudgeResult evaluate(JudgeTask task, TaskEvaluator evaluator, long timeoutMillis) {
        if (!prepared && !prepare()) {
            throw new SandboxExecutionException("El worker " + id + " no tiene entorno de ejecución", null);
        }
        Evaluation evaluation = new Evaluation(task, evaluator);
        evaluation.start();
        try {
            evaluation.join(timeoutMillis);
        } catch (InterruptedException e) {
            evaluation.interrupt();
            Thread.currentThread().interrupt();
            throw new SandboxExecutionException("Se interrumpió la evaluación del envío " + task.getSubmissionId(), e);
        }
        if (evaluation.isAlive()) {
            abandon(evaluation);
            throw new SandboxExecutionException("El worker " + id + " superó el tiempo máximo de " + timeoutMillis
                    + " ms evaluando el envío " + task.getSubmissionId(), null);
        }
        if (evaluation.failure != null) {
            throw evaluation.failure;
        }
        return evaluation.result;
    }

    /**
     * Libera lo que la última evaluación haya dejado en el entorno.
     *
     * @return {@code false} si quedó algo sin liberar y el worker debe reiniciarse.
     */
    boolean clean() {
        try {
            environment.clean();
            return true;
        } catch (IOException e) {
            log.warn("El worker {} no pudo liberar sus recursos: {}", id, e.toString());
            return false;
        }
    }

    /** Elimina el entorno del worker al detener el pool. */
    void destroy() {
        prepared = false;
        try {
            environment.destroy();
        } catch (IOException e) {
            log.error("No se pudo eliminar el entorno del worker {}: {}", id, e.toString());
        }
    }

    int id() {
        return id;
    }

    boolean isBusy() {
        return busy;
    }

    void setBusy(boolean busy) {
        this.busy = busy;
    }

    boolean isPrepared() {
        return prepared;
    }

    /**
     * Interrumpe el hilo de una evaluación colgada y le da un margen para que
     * termine, de modo que no siga tocando el entorno mientras el worker se
     * reinicia.
     */
    private void abandon(Evaluation evaluation) {
        evaluation.interrupt();
        try {
            evaluation.join(ABANDON_WAIT_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (evaluation.isAlive()) {
            log.error("El hilo del worker {} sigue vivo tras abandonar su evaluación", id);
        }
    }

    /** Hilo que ejecuta una evaluación y guarda su resultado o el fallo que la cortó. */
    private final class Evaluation extends Thread {

        private final JudgeTask task;
        private final TaskEvaluator evaluator;
        /** Resultado de la evaluación; {@code null} si falló. Se lee cuando el hilo ya terminó. */
        private JudgeResult result;
        /** Fallo que cortó la evaluación; {@code null} si terminó bien. Se lee cuando el hilo ya terminó. */
        private RuntimeException failure;

        Evaluation(JudgeTask task, TaskEvaluator evaluator) {
            super("judge-worker-" + id);
            setDaemon(true);
            this.task = task;
            this.evaluator = evaluator;
        }

        @Override
        public void run() {
            try {
                result = evaluator.evaluate(task, environment);
            } catch (RuntimeException e) {
                // Se conserva el tipo original: el listener decide con él si reintenta el envío.
                failure = e;
            } catch (Error e) {
                failure = new SandboxExecutionException(
                        "El worker " + id + " falló evaluando el envío " + task.getSubmissionId(), e);
            }
        }
    }
}
