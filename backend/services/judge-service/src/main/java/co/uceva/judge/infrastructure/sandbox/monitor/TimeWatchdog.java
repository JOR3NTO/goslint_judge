package co.uceva.judge.infrastructure.sandbox.monitor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicBoolean;

import co.uceva.judge.domain.valueobject.AbsoluteTimeLimit;
import co.uceva.judge.domain.valueobject.HardTimePercent;
import co.uceva.judge.domain.valueobject.WatchIntervalMillis;

/**
 * Hilo que vigila el tiempo de CPU consumido por el proceso en ejecución
 * dentro del sandbox, leyendo periódicamente las estadísticas expuestas por
 * el cgroup ({@code cpu.stat}). Si el proceso supera el límite de tiempo
 * permitido (con un margen de gracia) o el tiempo máximo absoluto de
 * ejecución, lo termina de forma forzada.
 */
public class TimeWatchdog extends Thread {

    /** Proceso cuyo tiempo de CPU está siendo vigilado. */
    private final Process p;
    /** Ruta del cgroup asociado al proceso, usada para forzar su terminación. */
    private final Path cgLeafPath;
    /** Ruta del archivo {@code cpu.stat} del cgroup, usado para leer el tiempo de CPU consumido. */
    private final Path cpuStatsPath;
    /** Límite de tiempo de ejecución, en milisegundos, configurado para el proceso. */
    private final long timeLimit;
    /** Porcentaje adicional sobre el límite de tiempo permitido antes de forzar la terminación del proceso. */
    private final HardTimePercent hardTimePercent;
    /** Intervalo con el que el watchdog verifica el tiempo de CPU utilizado por el proceso. */
    private final WatchIntervalMillis watchInterval;
    /** Tiempo máximo absoluto que puede durar un proceso, sin importar el límite configurado. */
    private final AbsoluteTimeLimit absoluteTimeLimit;
    /** Indica si el proceso fue terminado de forma forzada por exceder el límite de tiempo. */
    private final AtomicBoolean forcedTLE = new AtomicBoolean(false);

    /**
     * Crea el watchdog de tiempo para un proceso del sandbox, usando los
     * márgenes y el intervalo de vigilancia por defecto del dominio.
     *
     * @param p            Proceso cuyo tiempo de CPU será vigilado.
     * @param cpuStatsPath Ruta del archivo {@code cpu.stat} del cgroup del proceso.
     * @param timeLimit    Límite de tiempo de ejecución, en milisegundos, permitido para el proceso.
     * @param cgLeafPath   Ruta del cgroup del proceso, usada para terminarlo si es necesario.
     */
    public TimeWatchdog(Process p, Path cpuStatsPath, long timeLimit, Path cgLeafPath) {
        this(p, cpuStatsPath, timeLimit, cgLeafPath,
            HardTimePercent.ofDefault(), WatchIntervalMillis.ofDefault(), AbsoluteTimeLimit.ofDefault());
    }

    /**
     * Crea el watchdog de tiempo para un proceso del sandbox con márgenes
     * parametrizables, pensados para ser suministrados en el futuro desde una
     * API, en lugar de estar fijos en el código.
     *
     * @param p                 Proceso cuyo tiempo de CPU será vigilado.
     * @param cpuStatsPath      Ruta del archivo {@code cpu.stat} del cgroup del proceso.
     * @param timeLimit         Límite de tiempo de ejecución, en milisegundos, permitido para el proceso.
     * @param cgLeafPath        Ruta del cgroup del proceso, usada para terminarlo si es necesario.
     * @param hardTimePercent   Porcentaje adicional sobre el límite de tiempo antes de forzar la terminación.
     * @param watchInterval     Intervalo con el que se verifica el tiempo de CPU utilizado por el proceso.
     * @param absoluteTimeLimit Tiempo máximo absoluto que puede durar el proceso, sin importar el límite configurado.
     */
    public TimeWatchdog(Process p, Path cpuStatsPath, long timeLimit, Path cgLeafPath,
            HardTimePercent hardTimePercent, WatchIntervalMillis watchInterval, AbsoluteTimeLimit absoluteTimeLimit) {
        this.p = p;
        this.cpuStatsPath = cpuStatsPath;
        this.timeLimit = timeLimit;
        this.cgLeafPath = cgLeafPath;
        this.hardTimePercent = hardTimePercent;
        this.watchInterval = watchInterval;
        this.absoluteTimeLimit = absoluteTimeLimit;
    }

    /**
     * Mientras el proceso siga vivo, verifica cada {@code WATCH_INTERVAL_MS}
     * milisegundos el tiempo de CPU acumulado leído de {@code cpu.stat}. Si
     * dicho tiempo supera el límite configurado más el margen de gracia
     * ({@code HARD_TIME_PERCENT}), o si el tiempo total transcurrido supera
     * {@code MAX_TIME_MS}, marca el proceso como terminado por exceso de
     * tiempo, termina el cgroup y fuerza la destrucción del proceso y sus
     * descendientes.
     */
    @Override
    public void run() {
        long startInterval = System.currentTimeMillis();
        long startTime = System.nanoTime();
        while (p.isAlive()) {
            long current = System.currentTimeMillis();
            if (current - startInterval >= watchInterval.milliseconds()) {
                try {
                    startInterval = current;
                    long utime = Files.readAllLines(cpuStatsPath).stream()
                            .filter(line -> line.contains("usage_usec"))
                            .map(line -> line.split(" ")[1])
                            .mapToLong(Long::parseLong)
                            .sum();
                    if (utime >= timeLimit * (1 + hardTimePercent.percentage()) * 1000
                            || current - startTime >= absoluteTimeLimit.milliseconds()) {
                        forcedTLE.set(true);
                        killCgroup();
                        p.descendants().forEach(ProcessHandle::destroyForcibly);
                        p.destroyForcibly();
                        break;
                    }
                } catch (IOException e) {
                    System.out.println("Error reading CPU stats: " + e.getMessage());
                }
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                System.out.println("Error sleeping: " + e.getMessage());
            }
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
     * @return Indicador atómico de si el proceso fue terminado de forma
     *         forzada por exceder el límite de tiempo permitido.
     */
    public AtomicBoolean getForcedTLE() {
        return forcedTLE;
    }
}
