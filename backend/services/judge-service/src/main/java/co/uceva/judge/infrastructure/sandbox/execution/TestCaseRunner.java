package co.uceva.judge.infrastructure.sandbox.execution;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import co.uceva.judge.domain.valueobject.AbsoluteTimeLimit;
import co.uceva.judge.domain.valueobject.HardTimePercent;
import co.uceva.judge.domain.valueobject.OutputSizeLimit;
import co.uceva.judge.domain.valueobject.WatchIntervalMillis;
import co.uceva.judge.infrastructure.sandbox.monitor.ErrorsHandle;
import co.uceva.judge.infrastructure.sandbox.monitor.OutputHandle;
import co.uceva.judge.infrastructure.sandbox.monitor.TimeWatchdog;
import co.uceva.judge.infrastructure.sandbox.workspace.SandboxWorkspace;
import co.uceva.shared.domain.VerdictStatus;

/**
 * Ejecuta un único caso de prueba dentro del sandbox: lanza el proceso con el
 * comando de bwrap ya construido, le envía la entrada correspondiente y
 * monitorea su salida, errores y tiempo de ejecución mediante
 * {@link OutputHandle}, {@link ErrorsHandle} y {@link TimeWatchdog}. Al
 * finalizar, lee las métricas del cgroup y determina si algún límite fue
 * excedido.
 */
public class TestCaseRunner {

    /** Tamaño máximo permitido para la salida estándar y de error del proceso. */
    private final OutputSizeLimit maxOutputSize;
    /** Porcentaje adicional sobre el límite de tiempo antes de que el watchdog fuerce la terminación del proceso. */
    private final HardTimePercent hardTimePercent;
    /** Intervalo con el que el watchdog verifica el tiempo de CPU utilizado por el proceso. */
    private final WatchIntervalMillis watchInterval;
    /** Tiempo máximo absoluto que puede durar un proceso, sin importar el límite configurado. */
    private final AbsoluteTimeLimit absoluteTimeLimit;

    /**
     * Crea el ejecutor de casos de prueba con los límites que aplicará a cada
     * proceso lanzado.
     *
     * @param maxOutputSize     Tamaño máximo permitido para la salida estándar y de error del proceso.
     * @param hardTimePercent   Porcentaje adicional sobre el límite de tiempo antes de forzar la terminación del proceso.
     * @param watchInterval     Intervalo con el que se verifica el tiempo de CPU utilizado por el proceso.
     * @param absoluteTimeLimit Tiempo máximo absoluto que puede durar el proceso, sin importar el límite configurado.
     */
    public TestCaseRunner(OutputSizeLimit maxOutputSize, HardTimePercent hardTimePercent,
            WatchIntervalMillis watchInterval, AbsoluteTimeLimit absoluteTimeLimit) {
        this.maxOutputSize = maxOutputSize;
        this.hardTimePercent = hardTimePercent;
        this.watchInterval = watchInterval;
        this.absoluteTimeLimit = absoluteTimeLimit;
    }

    /**
     * Ejecuta el comando dentro del workspace dado, le escribe la entrada del
     * caso de prueba y espera su finalización.
     *
     * @param workspace  Workspace (cgroup y directorio de trabajo) del intento de ejecución.
     * @param runCommand Comando completo de bwrap a ejecutar, ya construido.
     * @param input      Entrada estándar a enviar al proceso.
     * @param timeLimit  Límite de tiempo de ejecución, en milisegundos, del caso de prueba.
     * @return El resultado del caso de prueba, con su veredicto de salida
     *         temprana (si lo hay) y las métricas observadas.
     * @throws IOException          Si ocurre un error de E/S lanzando el proceso o leyendo las métricas del cgroup.
     * @throws InterruptedException Si el hilo actual es interrumpido esperando la finalización del proceso.
     */
    public TestCaseResult run(SandboxWorkspace workspace, List<String> runCommand, String input, long timeLimit)
            throws IOException, InterruptedException {

        ProcessBuilder pb = new ProcessBuilder(runCommand);
        pb.directory(workspace.workDirPath().toFile());

        // Elimina las variables heredadas del proceso JVM.
        pb.environment().clear();

        // Entorno minimo para el shell externo.
        pb.environment().put("PATH", "/usr/bin:/bin");
        pb.environment().put("SECCOMP_PROFILE", workspace.seccompProfile());
        Process process = pb.start();
        ErrorsHandle errorsHandle = new ErrorsHandle(process, workspace.cgLeafPath(), process.getErrorStream(), maxOutputSize.bytes());
        OutputHandle outputHelper = new OutputHandle(workspace.cgLeafPath(), process.getInputStream(), maxOutputSize.bytes());
        outputHelper.start();
        errorsHandle.start();
        TimeWatchdog watchdog = new TimeWatchdog(process, workspace.cpuStatsPath(), timeLimit, workspace.cgLeafPath(),
                hardTimePercent, watchInterval, absoluteTimeLimit);
        watchdog.start();
        process.getOutputStream().write(input.getBytes(StandardCharsets.UTF_8));
        process.getOutputStream().close();
        process.waitFor();
        outputHelper.join();
        errorsHandle.join();
        watchdog.join();
        long memoryUsed = Long.parseLong(Files.readString(workspace.memoryPeakPath()).trim());
        long utime = Files.readAllLines(workspace.cpuStatsPath()).stream()
                .filter(line -> line.contains("usage_usec"))
                .map(line -> line.split(" ")[1])
                .mapToLong(Long::parseLong)
                .sum();
        boolean isOomKilled = Files.readAllLines(workspace.memoryEventsPath()).stream()
                .anyMatch(line -> line.contains("oom_kill") && !line.split(" ")[1].equals("0"));
        if (isOomKilled) {
            return new TestCaseResult(VerdictStatus.MEMORY_LIMIT_EXCEEDED, utime, memoryUsed, null);
        }
        if (watchdog.getForcedTLE().get() || utime > timeLimit) {
            return new TestCaseResult(VerdictStatus.TIME_LIMIT_EXCEEDED, utime, memoryUsed, null);
        }
        if (process.exitValue() != 0 || errorsHandle.getIsRuntimeErrorKilled().get()) {
            return new TestCaseResult(VerdictStatus.RUNTIME_ERROR, utime, memoryUsed, null);
        }
        if (outputHelper.getOutputSizeExceeded().get()) {
            // VerdictStatus no distingue un límite de salida excedido; se reporta como RUNTIME_ERROR.
            return new TestCaseResult(VerdictStatus.RUNTIME_ERROR, utime, memoryUsed, null);
        }
        return new TestCaseResult(null, utime, memoryUsed, outputHelper.getOutput().trim());
    }
}
