package co.uceva.judge.infrastructure.sandbox.workspace;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.uceva.judge.infrastructure.sandbox.kill.CgroupKiller;

/**
 * Recursos propios de un worker de evaluación: su rama de cgroups y su
 * directorio de trabajo. Todo lo que un worker crea al evaluar (las hojas
 * {@code prog-<uuid>} y los directorios de cada ejecución) cuelga de aquí, de
 * modo que nada de lo que hace un worker es visible para otro y basta vaciar
 * estas dos rutas para dejarlo como recién creado.
 */
public class WorkerEnvironment {

    private static final Logger log = LoggerFactory.getLogger(WorkerEnvironment.class);

    /** Controladores que las hojas {@code prog-*} necesitan heredar del cgroup del worker. */
    private static final String LEAF_CONTROLLERS = "+memory +pids";
    /** Un cgroup con procesos que aún están muriendo responde {@code EBUSY} al {@code rmdir}: se reintenta ~1 s. */
    private static final int REMOVE_ATTEMPTS = 20;
    private static final long REMOVE_RETRY_MS = 50;

    /** Cgroup del worker; las hojas de cada ejecución son sus hijas. */
    private final Path cgroupDir;
    /** Directorio del worker; los directorios de cada ejecución son sus hijos. */
    private final Path workDir;

    /**
     * @param cgroupDir Cgroup del worker.
     * @param workDir   Directorio de trabajo del worker.
     */
    public WorkerEnvironment(Path cgroupDir, Path workDir) {
        this.cgroupDir = cgroupDir;
        this.workDir = workDir;
    }

    /**
     * Entorno del worker {@code workerId}: {@code <cgroupRoot>/worker-<id>} y
     * {@code <workRoot>/worker-<id>}.
     *
     * @param cgroupRoot Rama de cgroups delegada al servicio ({@code /cg}).
     * @param workRoot   Raíz de los directorios de trabajo ({@code /work}).
     * @param workerId   Identificador del worker dentro del pool.
     * @return El entorno, aún sin crear en disco.
     */
    public static WorkerEnvironment forWorker(Path cgroupRoot, Path workRoot, int workerId) {
        String name = "worker-" + workerId;
        return new WorkerEnvironment(cgroupRoot.resolve(name), workRoot.resolve(name));
    }

    /**
     * Crea el cgroup y el directorio del worker partiendo de cero: lo que
     * hubiera de una vida anterior se elimina antes.
     *
     * @throws IOException Si no se puede limpiar el estado anterior o crear el entorno.
     */
    public void prepare() throws IOException {
        destroy();
        Files.createDirectories(workDir);
        Files.createDirectory(cgroupDir);
        Path subtreeControl = cgroupDir.resolve("cgroup.subtree_control");
        if (!Files.exists(subtreeControl)) {
            throw new IOException(cgroupDir + " no es un cgroup v2: revisa la rama delegada " + cgroupDir.getParent());
        }
        // Sin esto las hojas del worker nacen sin memory.max ni pids.max.
        Files.writeString(subtreeControl, LEAF_CONTROLLERS, StandardOpenOption.WRITE);
    }

    /**
     * Deja el entorno como recién creado: mata y borra las hojas de cgroup que
     * hayan quedado y vacía el directorio de trabajo.
     *
     * @throws IOException Si queda algo que no se puede eliminar.
     */
    public void clean() throws IOException {
        removeLeafCgroups();
        emptyDirectory(workDir);
    }

    /**
     * Elimina por completo el cgroup y el directorio del worker, con todo su
     * contenido.
     *
     * @throws IOException Si queda algo que no se puede eliminar.
     */
    public void destroy() throws IOException {
        if (Files.isDirectory(cgroupDir)) {
            removeLeafCgroups();
            removeCgroup(cgroupDir);
        }
        if (Files.isDirectory(workDir)) {
            emptyDirectory(workDir);
            Files.delete(workDir);
        }
    }

    /** @return Cgroup del worker, padre de las hojas de cada ejecución. */
    public Path cgroupDir() {
        return cgroupDir;
    }

    /** @return Directorio de trabajo del worker. */
    public Path workDir() {
        return workDir;
    }

    private void removeLeafCgroups() throws IOException {
        List<Path> leaves = new ArrayList<>();
        try (DirectoryStream<Path> children = Files.newDirectoryStream(cgroupDir, Files::isDirectory)) {
            children.forEach(leaves::add);
        }
        if (leaves.isEmpty()) {
            return;
        }
        log.debug("{}: quedaban {} hojas de cgroup; se eliminan", cgroupDir, leaves.size());
        // cgroup.kill en el cgroup del worker mata el subárbol entero de una vez.
        new CgroupKiller(cgroupDir).killCgroup();
        for (Path leaf : leaves) {
            removeCgroup(leaf);
        }
    }

    private static void removeCgroup(Path cgroup) throws IOException {
        IOException last = null;
        for (int attempt = 0; attempt < REMOVE_ATTEMPTS; attempt++) {
            try {
                Files.deleteIfExists(cgroup);
                return;
            } catch (IOException e) {
                last = e;
                try {
                    Thread.sleep(REMOVE_RETRY_MS);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new InterruptedIOException("Interrumpido borrando el cgroup " + cgroup);
                }
            }
        }
        throw new IOException("No se pudo borrar el cgroup " + cgroup, last);
    }

    private static void emptyDirectory(Path dir) throws IOException {
        List<Path> contents;
        try (var paths = Files.walk(dir)) {
            contents = paths.filter(path -> !path.equals(dir)).sorted(Comparator.reverseOrder()).toList();
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
        for (Path path : contents) {
            Files.deleteIfExists(path);
        }
    }
}
