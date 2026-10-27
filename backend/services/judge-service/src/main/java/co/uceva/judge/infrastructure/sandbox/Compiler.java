package co.uceva.judge.infrastructure.sandbox;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.uceva.judge.infrastructure.sandbox.command.CompilationCommandFactory;
import co.uceva.shared.domain.ProgrammingLanguage;

/**
 * Componente encargado de la compilación de las soluciones antes de su
 * ejecución dentro del sandbox. Pendiente de implementación.
 */
public class Compiler {

    private static final Logger log = LoggerFactory.getLogger(Compiler.class);

    public static int compile(ProgrammingLanguage language, String solutionPath, Path workDir) {
        List<String> command = CompilationCommandFactory.build(language, solutionPath, workDir);
        if (command == null) {
            return 0;
        }
        // Con la traza activa se captura lo que diga el compilador; con la traza
        // apagada se descarta, para no acumular en memoria la salida de un
        // compilador verboso cuando nadie va a leerla.
        boolean traceEnabled = log.isDebugEnabled();
        ProcessBuilder pb = new ProcessBuilder(command);
        if (traceEnabled) {
            pb.redirectErrorStream(true);
        } else {
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);
        }
        Process process = null;
        try {
            process = pb.start();
            // Hay que leer antes del waitFor: si el compilador llena el buffer de la
            // tuberia se queda bloqueado escribiendo y nunca termina.
            String compilerOutput = traceEnabled
                    ? new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8)
                    : "";
            int exitCode = process.waitFor();
            if (traceEnabled) {
                log.debug("compilación {} -> exitCode={} comando={}", language, exitCode, command);
                if (!compilerOutput.isBlank()) {
                    log.debug("salida del compilador:\n{}", compilerOutput);
                }
            }
            return exitCode;
        } catch (IOException ex) {
            // Falla el propio compilador, no el codigo evaluado: la traza va siempre.
            log.error("excepción compilando la solución {}", solutionPath, ex);
            return -1;
        } catch (InterruptedException ex) {
            log.error("compilación de la solución {} interrumpida", solutionPath, ex);
            Thread.currentThread().interrupt();
            return -1;
        } finally {
            // Si la evaluacion se abandona, el compilador no puede quedar vivo: corre
            // fuera del cgroup del worker y nadie mas lo mataria.
            if (process != null && process.isAlive()) {
                process.descendants().forEach(ProcessHandle::destroyForcibly);
                process.destroyForcibly();
            }
        }
    }
}
