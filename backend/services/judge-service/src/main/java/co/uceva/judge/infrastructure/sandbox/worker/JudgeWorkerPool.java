package co.uceva.judge.infrastructure.sandbox.worker;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.uceva.judge.application.port.out.SandboxExecutor;
import co.uceva.judge.domain.exception.SandboxExecutionException;
import co.uceva.judge.domain.model.JudgeResult;
import co.uceva.judge.domain.model.JudgeTask;
import co.uceva.judge.infrastructure.sandbox.workspace.WorkerEnvironment;
import co.uceva.shared.domain.VerdictStatus;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * Adaptador de {@link SandboxExecutor} que reparte las evaluaciones entre un
 * número fijo de workers independientes, cada uno con su propio
 * {@link WorkerEnvironment}.
 * <p>
 * Un envío se asigna a un worker libre y, si no hay ninguno, quien lo solicita
 * espera a que se libere uno. En el servicio hay tantos consumidores de RabbitMQ
 * como workers y cada uno toma un mensaje a la vez, así que los envíos que no
 * caben se quedan en la cola del broker y no en memoria.
 * </p>
 * <p>
 * Al terminar una evaluación el worker libera sus recursos y vuelve a estar
 * disponible. Si la evaluación falló, su entorno se crea de nuevo desde cero
 * antes de liberarlo: el fallo de un worker no reduce el tamaño del pool ni
 * afecta a los demás.
 * </p>
 */
public class JudgeWorkerPool implements SandboxExecutor {

    private static final Logger log = LoggerFactory.getLogger(JudgeWorkerPool.class);

    /** Cada cuánto se vuelve a buscar un worker libre cuando todos están ocupados. */
    private static final long FREE_WORKER_POLL_MS = 10;

    private final List<JudgeWorker> workers = new ArrayList<>();
    private final long evaluationTimeoutMillis;
    private final TaskEvaluator evaluator;
    /** {@code true} entre {@link #start()} y {@link #shutdown()}. */
    private volatile boolean running;

    /**
     * @param environments            Entorno de cada worker; el pool tiene un worker por entorno.
     * @param evaluationTimeoutMillis Tiempo máximo de una evaluación antes de dar al worker por colgado.
     * @param evaluator               Evaluación que ejecuta cada worker.
     */
    public JudgeWorkerPool(List<WorkerEnvironment> environments, long evaluationTimeoutMillis, TaskEvaluator evaluator) {
        if (environments.isEmpty()) {
            throw new IllegalArgumentException("El pool necesita al menos un worker");
        }
        if (evaluationTimeoutMillis <= 0) {
            throw new IllegalArgumentException("El tiempo máximo de una evaluación debe ser positivo");
        }
        for (WorkerEnvironment environment : environments) {
            workers.add(new JudgeWorker(workers.size() + 1, environment));
        }
        this.evaluationTimeoutMillis = evaluationTimeoutMillis;
        this.evaluator = evaluator;
    }

    /** Prepara el entorno de cada worker y los deja disponibles. */
    @PostConstruct
    public synchronized void start() {
        int ready = 0;
        for (JudgeWorker worker : workers) {
            if (worker.prepare()) {
                ready++;
            }
        }
        running = true;
        log.info("Pool de evaluación con {} workers, {} con su entorno listo (tiempo máximo por evaluación: {} s).",
                workers.size(), ready, evaluationTimeoutMillis / 1000);
    }

    /** Elimina el entorno de los workers libres; los ocupados lo eliminan al terminar su evaluación. */
    @PreDestroy
    public synchronized void shutdown() {
        running = false;
        for (JudgeWorker worker : workers) {
            if (!worker.isBusy()) {
                worker.destroy();
            }
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Espera hasta que haya un worker libre. Un worker que falla, devuelve
     * {@code JUDGE_ERROR} o supera el tiempo máximo se reinicia antes de quedar libre.
     * </p>
     */
    @Override
    public JudgeResult execute(JudgeTask task) {
        JudgeWorker worker = takeFreeWorker();
        boolean failed = true;
        try {
            log.debug("Envío {} asignado al worker {}", task.getSubmissionId(), worker.id());
            JudgeResult result = worker.evaluate(task, evaluator, evaluationTimeoutMillis);
            failed = result.getVerdict() == VerdictStatus.JUDGE_ERROR;
            return result;
        } finally {
            release(worker, failed);
        }
    }

    private JudgeWorker takeFreeWorker() {
        while (running) {
            JudgeWorker worker = markFreeWorkerAsBusy();
            if (worker != null) {
                return worker;
            }
            try {
                Thread.sleep(FREE_WORKER_POLL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new SandboxExecutionException("Interrumpido esperando un worker libre", e);
            }
        }
        throw new SandboxExecutionException("El pool de evaluación está detenido", null);
    }

    /** Buscar y marcar van juntos y sincronizados: así dos envíos nunca reciben el mismo worker. */
    private synchronized JudgeWorker markFreeWorkerAsBusy() {
        for (JudgeWorker worker : workers) {
            if (!worker.isBusy()) {
                worker.setBusy(true);
                return worker;
            }
        }
        return null;
    }

    private void release(JudgeWorker worker, boolean failed) {
        try {
            if (!running) {
                worker.destroy();
            } else if (failed) {
                log.warn("El worker {} falló durante una evaluación; se reinicia.", worker.id());
                worker.prepare();
            } else if (!worker.clean()) {
                worker.prepare();
            }
        } finally {
            // Pase lo que pase el worker queda libre: perderlo reduciría la capacidad para siempre.
            markAsFree(worker);
        }
    }

    private synchronized void markAsFree(JudgeWorker worker) {
        worker.setBusy(false);
    }
}
