package co.uceva.judge.infrastructure.sandbox.monitor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.uceva.judge.infrastructure.sandbox.Runner;
import co.uceva.judge.infrastructure.sandbox.kill.CgroupKiller;

/**
 * Hilo que consume el flujo de error estándar (stderr) del proceso en ejecución
 * dentro del sandbox. Valida cada línea contra los patrones de errores en
 * tiempo de ejecución sin retener la salida ya procesada y, al detectar un
 * error en ejecución, termina el cgroup del proceso.
 */
public class ErrorsHandle extends Thread {
    private static final Logger log = LoggerFactory.getLogger(ErrorsHandle.class);
    /** Flujo de error estándar (stderr) del proceso. */
    private final InputStream inputStream;
    /** Instancia del asistente para matar el cgroup del proceso. */
    private final CgroupKiller cgroupKiller;
    /** Tamaño máximo permitido para la salida de error (stderr) del proceso, en bytes. */
    private final long maxErrorSize;
    /** Indica si el proceso fue terminado por detectarse un error en tiempo de ejecución. */
    private final AtomicBoolean isRuntimeErrorKilled = new AtomicBoolean(false);
    /** Indica si el proceso fue terminado por detectarse un fallo propio del sandbox (p. ej. {@code bwrap}). */
    private final AtomicBoolean isSandboxErrorKilled = new AtomicBoolean(false);
    /** Indica si el proceso fue terminado por superar el límite de tamaño de stderr. */
    private final AtomicBoolean isErrorSizeExceeded = new AtomicBoolean(false);
    /**
     * Tamaño máximo de stderr que se retiene para la traza de depuración. Es
     * independiente de {@code maxErrorSize}: ese es el límite que se le impone a la
     * solución, este solo acota cuánto se guarda para poder volcarlo en el log.
     */
    private static final int TRACE_STDERR_CAP = 8 * 1024;
    /**
     * Fragmento inicial de stderr, retenido <strong>solo</strong> si la traza de
     * depuración del sandbox está activa. Con la traza apagada queda vacío y la
     * clase sigue sin conservar la salida ya procesada, como está diseñada.
     */
    private final StringBuilder tracedStderr = new StringBuilder();
    /** Se evalúa una vez por ejecución: con la traza apagada no se retiene nada. */
    private final boolean traceEnabled = log.isDebugEnabled();

    /**
     * Crea el manejador de errores para un proceso del sandbox.
     *
     * @param cgLeafPath    Ruta del cgroup del proceso, usada para terminarlo si es necesario.
     * @param inputStream   Flujo de error estándar (stderr) del proceso.
     * @param maxErrorSize  Tamaño máximo permitido para la salida de error, en bytes.
     */
    public ErrorsHandle(Path cgLeafPath, InputStream inputStream, long maxErrorSize) {
        this.inputStream = inputStream;
        this.cgroupKiller = new CgroupKiller(cgLeafPath);
        this.maxErrorSize = maxErrorSize;
    }

    /**
     * Lee de forma continua el flujo de error del proceso mientras este siga vivo.
     * Los patrones de {@link ErrorsHandlePredicate} solo pueden coincidir dentro de
     * una única línea, así que no hace falta conservar la salida completa: se
     * valida cada línea completa contra ellos en cuanto llega y se descarta,
     * reteniendo únicamente el fragmento incompleto de la última línea en espera
     * de su salto de línea. Si alguna línea coincide con un patrón de error en
     * tiempo de ejecución, marca el proceso como terminado por error y fuerza la
     * finalización del cgroup.
     */
    @Override
    public void run() {
        try {
            byte[] buffer = new byte[1024];
            int bytesRead;
            long totalBytesRead = 0;
            String pendingLine = "";

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                totalBytesRead += bytesRead;
                if (totalBytesRead > maxErrorSize) {
                    log.debug("stderr excedió el límite de {} bytes (leídos {})", maxErrorSize, totalBytesRead);
                    isErrorSizeExceeded.set(true);
                    cgroupKiller.killCgroup();
                    break;
                }
                String chunk = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
                if (traceEnabled && tracedStderr.length() < TRACE_STDERR_CAP) {
                    tracedStderr.append(chunk);
                }
                pendingLine += chunk;
                List<String> lines = Arrays.asList(pendingLine.split("\n", -1));
                // El último elemento es el fragmento sin salto de línea aún: se conserva para la siguiente lectura.
                pendingLine = lines.get(lines.size() - 1);
                List<String> completedLines = lines.subList(0, lines.size() - 1);

                boolean isSandboxFailure = completedLines.stream().anyMatch(ErrorsHandlePredicate.SANDBOX_FAILURE);
                boolean hasErrors = completedLines.stream().anyMatch(ErrorsHandlePredicate.ERROR_RUNTIME);
                if (isSandboxFailure) {
                    // Un fallo de bwrap es un problema de la plataforma, no del código
                    // evaluado: se registra siempre, no solo con la traza activa.
                    completedLines.stream().filter(ErrorsHandlePredicate.SANDBOX_FAILURE)
                            .forEach(line -> log.warn("fallo del sandbox detectado en stderr: {}", line));
                    isSandboxErrorKilled.set(true);
                    cgroupKiller.killCgroup();
                    break;
                }
                if (hasErrors) {
                    completedLines.stream().filter(ErrorsHandlePredicate.ERROR_RUNTIME)
                            .forEach(line -> log.debug("excepción/runtime error detectado en stderr: {}", line));
                    isRuntimeErrorKilled.set(true);
                    cgroupKiller.killCgroup();
                    break;
                }
            }
        } catch (IOException e) {
            log.error("Error reading process output: {}", e.getMessage(), e);
            isRuntimeErrorKilled.set(true);
            cgroupKiller.killCgroup();
        }
    }

    /**
     * @return Fragmento inicial de stderr retenido para la traza de depuración,
     *         o cadena vacía si la traza del sandbox está desactivada.
     */
    public String getTracedStderr() {
        return tracedStderr.toString();
    }

    /** @return Indicador atómico de si el proceso fue terminado por superar el límite de tamaño de stderr. */
    public AtomicBoolean getIsErrorSizeExceeded() {
        return isErrorSizeExceeded;
    }

    /**
     * @return Indicador atómico de si el proceso fue terminado por detectarse
     *         un error en tiempo de ejecución.
     */
    public AtomicBoolean getIsRuntimeErrorKilled() {
        return isRuntimeErrorKilled;
    }

    /**
     * @return Indicador atómico de si el proceso fue terminado por detectarse
     *         un fallo propio del sandbox (p. ej. {@code bwrap}), y no un
     *         error del código del estudiante.
     */
    public AtomicBoolean getIsSandboxErrorKilled() {
        return isSandboxErrorKilled;
    }
}
