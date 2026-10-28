package co.uceva.judge.infrastructure.sandbox;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import co.uceva.judge.domain.exception.SandboxExecutionException;
import co.uceva.judge.domain.model.JudgeResult;
import co.uceva.judge.domain.model.JudgeTask;
import co.uceva.judge.infrastructure.sandbox.command.RunCommandFactory;
import co.uceva.judge.infrastructure.sandbox.worker.TaskEvaluator;
import co.uceva.judge.infrastructure.sandbox.workspace.SolutionFileWriter;
import co.uceva.judge.infrastructure.sandbox.workspace.WorkerEnvironment;
import co.uceva.shared.domain.VerdictStatus;

/**
 * Evaluación de una tarea sobre {@link Runner}: escribe el código fuente en un
 * directorio temporal del worker, lo compila, lo ejecuta contra los casos de
 * prueba y traduce el mapa de resultados del {@code Runner} a un
 * {@link JudgeResult}.
 * <p>
 * No guarda estado entre evaluaciones: todo lo que crea vive en el
 * {@link WorkerEnvironment} recibido, así que varios workers pueden compartir
 * la misma instancia.
 * </p>
 */
@Component
public class RunnerSandboxExecutor implements TaskEvaluator {

    private static final Logger log = LoggerFactory.getLogger(RunnerSandboxExecutor.class);

    private final Runner runner;

    /**
     * @param runner Orquestador de la ejecución dentro del sandbox.
     */
    public RunnerSandboxExecutor(Runner runner) {
        this.runner = runner;
    }

    /**
     * {@inheritDoc}
     *
     * @throws SandboxExecutionException Si no se puede preparar el archivo fuente o el
     *                                   {@code Runner} devuelve un resultado incompleto.
     */
    @Override
    public JudgeResult evaluate(JudgeTask task, WorkerEnvironment environment) {
        Path workDir = null;
        try {
            workDir = Files.createTempDirectory(environment.workDir(), "src-");
            log.debug("evaluando envío {} en {} ({} casos de prueba)",
                    task.getSubmissionId(), task.getLanguage(), task.getTestCases().size());
            String source = SolutionFileWriter.execute(task.getLanguage(), task.getSourceCode().content(), workDir, task.getSubmissionId());
            int exitCompilation = Compiler.compile(task.getLanguage(), source, workDir);
            if(exitCompilation != 0){
                log.debug("veredicto COMPILATION_ERROR para el envío {}", task.getSubmissionId());
                return JudgeResult.create(
                    task.getSubmissionId(),
                    VerdictStatus.COMPILATION_ERROR,
                    0,
                    0,
                    null
                ); 
            }

            
            Map<String, Object> result = runner.runSolution(environment,
                    RunCommandFactory.build(task.getLanguage(), source),
                    source, task.getTestCases(),
                    task.getTimeLimit().milliseconds(), task.getMemoryLimit().kilobytes() * 1024L);
            return toJudgeResult(task.getSubmissionId(), result);
        } catch (IOException e) {
            // La traza va siempre: es un fallo de la plataforma preparando el envio.
            log.error("fallo preparando el código fuente del envío {}", task.getSubmissionId(), e);
            throw new SandboxExecutionException("No se pudo preparar el código fuente del envío " + task.getSubmissionId(), e);
        } finally {
            deleteQuietly(workDir);
        }
    }

    private JudgeResult toJudgeResult(UUID submissionId, Map<String, Object> result) {
        VerdictStatus verdict = (VerdictStatus) result.get("status");
        Number cpuTimeMs = (Number) result.get("maxCpuTime");
        Number memoryKb = (Number) result.get("maxMemoryUsed");
        log.debug("resultado crudo del Runner para el envío {}: {}", submissionId, result);
        if (verdict == null || cpuTimeMs == null || memoryKb == null) {
            throw new SandboxExecutionException("El Runner devolvió un resultado incompleto: " + result, null);
        }
        return JudgeResult.create(submissionId, verdict, clampToInt(cpuTimeMs), clampToInt(memoryKb),
                (UUID) result.get("failedTestCase"));
    }

    private static int clampToInt(Number value) {
        return (int) Math.max(0, Math.min(value.longValue(), Integer.MAX_VALUE));
    }

    private static void deleteQuietly(Path dir) {
        if (dir == null) {
            return;
        }
        try (var paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
        } catch (IOException ignored) {
            // limpieza de mejor esfuerzo: un temporal huérfano no invalida el resultado
        }
    }
}
