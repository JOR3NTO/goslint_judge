package co.uceva.judge.infrastructure.config;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import co.uceva.judge.domain.repository.MonitorLimitsRepository;
import co.uceva.judge.domain.valueobject.PidsLimit;
import co.uceva.judge.domain.valueobject.VolumeSizeLimit;
import co.uceva.judge.infrastructure.sandbox.Runner;
import co.uceva.judge.infrastructure.sandbox.worker.JudgeWorkerPool;
import co.uceva.judge.infrastructure.sandbox.worker.TaskEvaluator;
import co.uceva.judge.infrastructure.sandbox.workspace.WorkerEnvironment;

/** Configuración del sandbox de ejecución. */
@Configuration
public class SandboxConfig {

    /**
     * Runner con los límites de procesos y volumen por defecto y los límites de
     * los monitores leídos del repositorio en cada ejecución.
     *
     * @param monitorLimitsRepository Fuente de los límites de los monitores.
     * @return El orquestador de ejecución dentro del sandbox.
     */
    @Bean
    public Runner runner(MonitorLimitsRepository monitorLimitsRepository) {
        return new Runner(PidsLimit.ofDefault(), VolumeSizeLimit.ofDefault(), monitorLimitsRepository);
    }

    /**
     * Pool de workers que evalúa los envíos en paralelo. Su tamaño es también
     * el número de consumidores de RabbitMQ (ver {@code application.properties}).
     *
     * @param workers                  Número de workers.
     * @param evaluationTimeoutSeconds Tiempo máximo de una evaluación, en segundos.
     * @param cgroupRoot               Rama de cgroups delegada al servicio.
     * @param workRoot                 Raíz de los directorios de trabajo.
     * @param evaluator                Evaluación que ejecuta cada worker.
     * @return El pool, con un worker por entorno, que Spring arranca y detiene con el contexto.
     */
    @Bean
    public JudgeWorkerPool judgeWorkerPool(
            @Value("${app.sandbox.workers.count:2}") int workers,
            @Value("${app.sandbox.workers.evaluation-timeout-seconds:300}") long evaluationTimeoutSeconds,
            @Value("${app.sandbox.cgroup-root:/cg}") String cgroupRoot,
            @Value("${app.sandbox.work-root:/work}") String workRoot,
            TaskEvaluator evaluator) {
        List<WorkerEnvironment> environments = new ArrayList<>();
        for (int workerId = 1; workerId <= workers; workerId++) {
            environments.add(WorkerEnvironment.forWorker(Path.of(cgroupRoot), Path.of(workRoot), workerId));
        }
        return new JudgeWorkerPool(environments, evaluationTimeoutSeconds * 1000, evaluator);
    }
}
