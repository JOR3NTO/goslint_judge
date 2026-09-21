package co.uceva.judge.infrastructure.sandbox.monitor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.uceva.judge.infrastructure.sandbox.Runner;
import co.uceva.judge.infrastructure.sandbox.kill.CgroupKiller;

/**
 * Hilo que consume el flujo de error estándar (stderr) del proceso en ejecución
 * dentro del sandbox. Acumula la salida de error, detecta patrones de errores
 * de tiempo de ejecución y, en caso de superar el tamaño máximo permitido o de
 * detectar un error en ejecución, termina el cgroup del proceso.
 */
public class ErrorsHandle extends Thread {
    private static final Logger log = LoggerFactory.getLogger(ErrorsHandle.class);
    /** Flujo de error estándar (stderr) del proceso. */
    private final InputStream inputStream;
    /** Tamaño máximo en bytes permitido para la salida de error. */
    private final long maxErrorsSize;
    /** Acumulador de la salida de error leída hasta el momento. */
    private final StringBuilder errorsBuilder = new StringBuilder();
    /** Indica si la salida de error acumulada coincide con un patrón de error en ejecución. */
    private boolean hasErrors;
    /** Instancia del asistente para matar el cgroup del proceso. */
    private final CgroupKiller cgroupKiller;
    /** Indica si el proceso fue terminado por detectarse un error en tiempo de ejecución. */
    private final AtomicBoolean isRuntimeErrorKilled = new AtomicBoolean(false);

    /**
     * Crea el manejador de errores para un proceso del sandbox.
     *
     * @param cgLeafPath    Ruta del cgroup del proceso, usada para terminarlo si es necesario.
     * @param inputStream   Flujo de error estándar (stderr) del proceso.
     * @param maxErrorsSize Tamaño máximo en bytes permitido para la salida de error.
     */
    public ErrorsHandle(Path cgLeafPath, InputStream inputStream, long maxErrorsSize) {
        this.inputStream = inputStream;
        this.maxErrorsSize = maxErrorsSize;
        this.cgroupKiller = new CgroupKiller(cgLeafPath);
    }

    /**
     * Lee de forma continua el flujo de error del proceso mientras este siga vivo.
     * Si el total de bytes leídos supera {@code maxErrorsSize} o la salida acumulada
     * coincide con algún patrón de error en tiempo de ejecución definido en
     * {@link ErrorsHandlePredicate}, marca el proceso como terminado por error y
     * fuerza la finalización del cgroup.
     */
    @Override
    public void run() {
        try {
            byte[] buffer = new byte[1024];
            int bytesRead;
            long totalBytesRead = 0;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                totalBytesRead += bytesRead;
                errorsBuilder.append(new String(buffer, 0, bytesRead, StandardCharsets.UTF_8));
                hasErrors = Arrays.asList(errorsBuilder.toString().split("\n")).stream().
                    anyMatch(ErrorsHandlePredicate.ERROR_RUNTIME);
                if (totalBytesRead > maxErrorsSize || hasErrors) {
                    isRuntimeErrorKilled.set(true);
                    cgroupKiller.killCgroup();
                    break;
                }

            }
        } catch (IOException e) {
            log.error("Error reading process output: {}", e.getMessage(), e);
        }
    }

    /**
     * @return Indicador atómico de si el proceso fue terminado por detectarse
     *         un error en tiempo de ejecución.
     */
    public AtomicBoolean getIsRuntimeErrorKilled() {
        return isRuntimeErrorKilled;
    }
}
