package co.uceva.judge.infrastructure.sandbox.workspace;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.UUID;

import co.uceva.judge.domain.valueobject.PidsLimit;

/**
 * Representa el entorno aislado (cgroup y directorio de trabajo) creado para
 * un único intento de ejecución dentro del sandbox. Se encarga de crear ese
 * entorno y, una vez terminado el intento, de limpiarlo.
 */
public class SandboxWorkspace {

    /** Ruta del cgroup creado para la ejecución en curso. */
    private final Path cgLeafPath;
    /** Ruta del archivo {@code memory.peak} del cgroup, con el pico de memoria usado. */
    private final Path memoryPeakPath;
    /** Ruta del archivo {@code memory.events} del cgroup, con los eventos de memoria (por ejemplo, OOM). */
    private final Path memoryEventsPath;
    /** Ruta del archivo {@code cpu.stat} del cgroup, con el tiempo de CPU consumido. */
    private final Path cpuStatsPath;
    /** Ruta del archivo {@code cgroup.procs} del cgroup, usado para inscribir el proceso del sandbox. */
    private final Path cgroupProcs;
    /** Ruta del directorio de trabajo de la ejecución en curso. */
    private final Path workDirPath;
    /** Directorio de trabajo escribible asignado a la ejecución en curso. */
    private final String workDir;
    /** Ruta del perfil de seccomp aplicado dentro del sandbox. */
    private final String seccompProfile;
    /** Ruta absoluta del archivo fuente de la solución a ejecutar. */
    private final Path origen;

    private SandboxWorkspace(Path cgLeafPath, Path memoryPeakPath, Path memoryEventsPath, Path cpuStatsPath,
            Path cgroupProcs, Path workDirPath, String workDir, String seccompProfile, Path origen) {
        this.cgLeafPath = cgLeafPath;
        this.memoryPeakPath = memoryPeakPath;
        this.memoryEventsPath = memoryEventsPath;
        this.cpuStatsPath = cpuStatsPath;
        this.cgroupProcs = cgroupProcs;
        this.workDirPath = workDirPath;
        this.workDir = workDir;
        this.seccompProfile = seccompProfile;
        this.origen = origen;
    }

    /**
     * Crea el cgroup y el directorio de trabajo de un nuevo intento de
     * ejecución: configura los límites de memoria y de número de procesos del
     * cgroup, crea el directorio de trabajo escribible y copia en él el
     * archivo fuente de la solución.
     *
     * @param memoryLimit  Límite de memoria, en bytes, asignado al cgroup.
     * @param maxPids      Número máximo de procesos simultáneos permitidos dentro del sandbox.
     * @param solutionPath Ruta del archivo fuente de la solución a ejecutar.
     * @return El workspace listo para ejecutar la solución dentro de él.
     * @throws IOException Si ocurre un error creando el cgroup, el directorio
     *                      de trabajo o copiando el archivo de la solución.
     */
    public static SandboxWorkspace create(long memoryLimit, PidsLimit maxPids, String solutionPath) throws IOException {
        String cgLeaf = "/cg/prog-" + UUID.randomUUID().toString();
        String memoryPeak = cgLeaf + "/memory.peak";
        String memoryEvents = cgLeaf + "/memory.events";
        String memoryMax = cgLeaf + "/memory.max";
        String pidsMax = cgLeaf + "/pids.max";
        String cpuStats = cgLeaf + "/cpu.stat";
        Path cpuStatsPath = Path.of(cpuStats);
        Path cgLeafPath = Path.of(cgLeaf);
        Path memoryPeakPath = Path.of(memoryPeak);
        Path memoryEventsPath = Path.of(memoryEvents);
        Path memoryMaxPath = Path.of(memoryMax);
        Path pidsMaxPath = Path.of(pidsMax);
        cgLeafPath.toFile().mkdirs();
        cgLeafPath.toFile().mkdirs();
        Files.writeString(memoryMaxPath, String.valueOf(memoryLimit), StandardOpenOption.WRITE);
        Files.writeString(pidsMaxPath, String.valueOf(maxPids.pids()), StandardOpenOption.WRITE);
        String uuid = UUID.randomUUID().toString();
        String workDir = "/work/" + uuid;
        Path cgroupProcs = Path.of(cgLeaf + "/cgroup.procs");
        String seccompProfile = "/opt/judge/filter.bpf";
        Path workDirPath = Path.of(workDir);

        if (!workDirPath.toFile().exists()) {
            workDirPath.toFile().mkdirs();
        }
        Path origen = Path.of(solutionPath).toAbsolutePath();
        Files.copy(origen, Path.of(workDir, origen.getFileName().toString()));

        return new SandboxWorkspace(cgLeafPath, memoryPeakPath, memoryEventsPath, cpuStatsPath, cgroupProcs,
                workDirPath, workDir, seccompProfile, origen);
    }

    /**
     * Elimina los archivos del directorio de trabajo y, únicamente, el
     * directorio del cgroup de este intento de ejecución. Los errores al
     * eliminar se registran pero no interrumpen la limpieza del resto de
     * archivos.
     */
    public void cleanup() {
        // Eliminar archivos normales del directorio de trabajo
        if (workDirPath != null && Files.exists(workDirPath)) {
            try (var paths = Files.walk(workDirPath)) {
                paths.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            System.err.println(
                                "Error deleting work path: "
                                + path + " - " + e
                            );
                        }
                    });
            } catch (IOException e) {
                System.err.println("Error walking work directory: " + e);
            }
        }

        // Eliminar unicamente el directorio del cgroup
        if (cgLeafPath != null) {
            try {
                Files.deleteIfExists(cgLeafPath);
            } catch (IOException e) {
                System.err.println(
                    "Error deleting cgroup directory: "
                    + cgLeafPath + " - " + e
                );
            }
        }
    }

    /** @return Ruta del cgroup de la ejecución en curso. */
    public Path cgLeafPath() {
        return cgLeafPath;
    }

    /** @return Ruta del archivo {@code memory.peak} del cgroup. */
    public Path memoryPeakPath() {
        return memoryPeakPath;
    }

    /** @return Ruta del archivo {@code memory.events} del cgroup. */
    public Path memoryEventsPath() {
        return memoryEventsPath;
    }

    /** @return Ruta del archivo {@code cpu.stat} del cgroup. */
    public Path cpuStatsPath() {
        return cpuStatsPath;
    }

    /** @return Ruta del archivo {@code cgroup.procs} del cgroup. */
    public Path cgroupProcs() {
        return cgroupProcs;
    }

    /** @return Ruta del directorio de trabajo de la ejecución en curso. */
    public Path workDirPath() {
        return workDirPath;
    }

    /** @return Directorio de trabajo escribible asignado a la ejecución en curso. */
    public String workDir() {
        return workDir;
    }

    /** @return Ruta del perfil de seccomp aplicado dentro del sandbox. */
    public String seccompProfile() {
        return seccompProfile;
    }

    /** @return Ruta absoluta del archivo fuente de la solución a ejecutar. */
    public Path origen() {
        return origen;
    }
}
