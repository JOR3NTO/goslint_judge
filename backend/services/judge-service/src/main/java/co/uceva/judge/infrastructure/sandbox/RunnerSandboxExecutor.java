package co.uceva.judge.infrastructure.sandbox;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import co.uceva.judge.application.port.out.SandboxExecutor;
import co.uceva.judge.domain.exception.SandboxExecutionException;
import co.uceva.judge.domain.model.JudgeResult;
import co.uceva.judge.domain.model.JudgeTask;
import co.uceva.shared.domain.ProgrammingLanguage;
import co.uceva.shared.domain.VerdictStatus;

/**
 * Adaptador de {@link SandboxExecutor} sobre {@link Runner}: escribe el código
 * fuente en un directorio temporal, lo ejecuta contra los casos de prueba y
 * traduce el mapa de resultados del {@code Runner} a un {@link JudgeResult}.
 * <p>
 * Por ahora solo se soportan lenguajes interpretados. La compilación de C, C++
 * y Java llegará con el {@code Compiler}; mientras tanto esos lenguajes fallan
 * como error del sistema y no como veredicto, para no culpar al estudiante de
 * una limitación de la plataforma.
 * </p>
 */
@Component
public class RunnerSandboxExecutor implements SandboxExecutor {

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
     * @throws SandboxExecutionException Si el lenguaje aún no está soportado, no se puede
     *                                   preparar el archivo fuente o el {@code Runner} devuelve un resultado incompleto.
     */
    @Override
    public JudgeResult execute(JudgeTask task) {
        ProgrammingLanguage language = task.getLanguage();
        if (language != ProgrammingLanguage.PYTHON) {
            throw new SandboxExecutionException("El lenguaje " + language + " aún no está soportado por el sandbox.", null);
        }

        Path workDir = null;
        try {
            workDir = Files.createTempDirectory("judge-" + task.getSubmissionId() + "-");
            Path source = workDir.resolve("solution.py");
            Files.writeString(source, task.getSourceCode().content(), StandardCharsets.UTF_8);

            Map<String, Object> result = runner.runSolution("python3", source.toString(), task.getTestCases(),
                    task.getTimeLimit().milliseconds(), task.getMemoryLimit().kilobytes() * 1024L);
            return toJudgeResult(task.getSubmissionId(), result);
        } catch (IOException e) {
            throw new SandboxExecutionException("No se pudo preparar el código fuente del envío " + task.getSubmissionId(), e);
        } finally {
            deleteQuietly(workDir);
        }
    }

    private JudgeResult toJudgeResult(UUID submissionId, Map<String, Object> result) {
        VerdictStatus verdict = (VerdictStatus) result.get("status");
        Number cpuTimeMs = (Number) result.get("maxCpuTime");
        Number memoryKb = (Number) result.get("maxMemoryUsed");
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
