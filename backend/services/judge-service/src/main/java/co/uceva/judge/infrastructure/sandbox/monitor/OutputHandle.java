package co.uceva.judge.infrastructure.sandbox.monitor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Hilo que consume el flujo de salida estándar (stdout) del proceso en
 * ejecución dentro del sandbox. Acumula la salida producida y, si supera el
 * tamaño máximo permitido, termina el cgroup del proceso.
 */
public class OutputHandle extends Thread {
    /** Flujo de salida estándar (stdout) del proceso. */
    private final InputStream inputStream;
    /** Tamaño máximo en bytes permitido para la salida estándar. */
    private final long maxOutputSize;
    /** Ruta del cgroup asociado al proceso, usada para forzar su terminación. */
    private final Path cgLeafPath;
    /** Acumulador de la salida estándar leída hasta el momento. */
    private final StringBuilder outputBuilder = new StringBuilder();
    /** Indica si la salida estándar superó el tamaño máximo permitido. */
    private final AtomicBoolean outputSizeExceeded = new AtomicBoolean(false);
    /** Indica si la lectura del flujo de salida ha finalizado. */
    private final AtomicBoolean outputReadComplete = new AtomicBoolean(false);

    /**
     * Crea el manejador de salida para un proceso del sandbox.
     *
     * @param cgLeafPath    Ruta del cgroup del proceso, usada para terminarlo si es necesario.
     * @param inputStream   Flujo de salida estándar (stdout) del proceso.
     * @param maxOutputSize Tamaño máximo en bytes permitido para la salida estándar.
     */
    public OutputHandle(Path cgLeafPath, InputStream inputStream, long maxOutputSize) {
        this.inputStream = inputStream;
        this.maxOutputSize = maxOutputSize;
        this.cgLeafPath = cgLeafPath;
    }

    /**
     * Lee de forma continua el flujo de salida del proceso hasta que este se
     * cierre. Si el total de bytes leídos supera {@code maxOutputSize}, marca
     * la salida como excedida y fuerza la finalización del cgroup.
     */
    @Override
    public void run() {
        try {
            byte[] buffer = new byte[1024];
            int bytesRead;
            long totalBytesRead = 0;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                totalBytesRead += bytesRead;
                if (totalBytesRead > maxOutputSize) {
                    outputSizeExceeded.set(true);
                    killCgroup();
                    break;
                }
                outputBuilder.append(new String(buffer, 0, bytesRead, StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            System.out.println("Error reading process output: " + e.getMessage());
        } finally {
            outputReadComplete.set(true);
        }
    }

    /**
     * Escribe en {@code cgroup.kill} para forzar la terminación de todos los
     * procesos pertenecientes al cgroup del proceso monitoreado.
     */
    private void killCgroup() {
        if (cgLeafPath == null) {
            return;
        }

        Path cgroupKillPath = cgLeafPath.resolve("cgroup.kill");

        try {
            if (Files.exists(cgroupKillPath)) {
                Files.writeString(
                    cgroupKillPath,
                    "1",
                    StandardOpenOption.WRITE
                );
            }
        } catch (IOException e) {
            System.err.println(
                "Error killing cgroup " + cgLeafPath + ": " + e
            );
        }
    }

    /**
     * @return La salida estándar acumulada del proceso hasta el momento.
     */
    public String getOutput() {
        return outputBuilder.toString();
    }

    /**
     * @return Indicador atómico de si la salida estándar superó el tamaño máximo permitido.
     */
    public AtomicBoolean getOutputSizeExceeded() {
        return outputSizeExceeded;
    }

    /**
     * @return Indicador atómico de si la lectura del flujo de salida ha finalizado.
     */
    public AtomicBoolean getOutputReadComplete() {
        return outputReadComplete;
    }
}
