package co.uceva.judge.infrastructure.sandbox.workspace;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sobre un sistema de archivos normal: los cgroups se simulan con directorios
 * vacíos, que es lo único que {@code rmdir} acepta fuera del cgroupfs. La
 * creación real de la rama solo se puede probar dentro del contenedor.
 */
class WorkerEnvironmentTest {

    @TempDir Path tempDir;

    private Path cgroupRoot;
    private Path workRoot;
    private WorkerEnvironment environment;

    @BeforeEach
    void setUp() throws IOException {
        cgroupRoot = Files.createDirectory(tempDir.resolve("cg"));
        workRoot = Files.createDirectory(tempDir.resolve("work"));
        environment = WorkerEnvironment.forWorker(cgroupRoot, workRoot, 3);
    }

    @Test
    void shouldGiveEachWorkerItsOwnPaths() {
        WorkerEnvironment other = WorkerEnvironment.forWorker(cgroupRoot, workRoot, 4);

        assertThat(environment.cgroupDir()).isEqualTo(cgroupRoot.resolve("worker-3"));
        assertThat(environment.workDir()).isEqualTo(workRoot.resolve("worker-3"));
        assertThat(other.cgroupDir()).isNotEqualTo(environment.cgroupDir());
        assertThat(other.workDir()).isNotEqualTo(environment.workDir());
    }

    @Test
    void shouldRefuseToPrepareOutsideACgroupHierarchy() {
        assertThatThrownBy(environment::prepare).isInstanceOf(IOException.class).hasMessageContaining("cgroup v2");
    }

    @Test
    void shouldRemoveLeftoversWhenCleaning() throws IOException {
        Files.createDirectories(environment.cgroupDir().resolve("prog-huerfana"));
        Path leftover = Files.createDirectories(environment.workDir().resolve("src-123"));
        Files.writeString(leftover.resolve("solution.py"), "print(1)");

        environment.clean();

        assertThat(environment.cgroupDir()).isEmptyDirectory();
        assertThat(environment.workDir()).isEmptyDirectory();
    }

    @Test
    void shouldNotTouchOtherWorkersWhenCleaning() throws IOException {
        WorkerEnvironment other = WorkerEnvironment.forWorker(cgroupRoot, workRoot, 4);
        Files.createDirectories(environment.cgroupDir());
        Files.createDirectories(environment.workDir());
        Path otherLeaf = Files.createDirectories(other.cgroupDir().resolve("prog-en-curso"));
        Path otherFile = Files.writeString(Files.createDirectories(other.workDir()).resolve("solution.py"), "print(2)");

        environment.clean();

        assertThat(otherLeaf).exists();
        assertThat(otherFile).exists();
    }

    @Test
    void shouldRemoveEverythingWhenDestroyed() throws IOException {
        Files.createDirectories(environment.cgroupDir().resolve("prog-huerfana"));
        Files.writeString(Files.createDirectories(environment.workDir()).resolve("solution.py"), "print(1)");

        environment.destroy();

        assertThat(environment.cgroupDir()).doesNotExist();
        assertThat(environment.workDir()).doesNotExist();
    }

    @Test
    void shouldDestroyAnEnvironmentThatWasNeverCreated() throws IOException {
        environment.destroy();

        assertThat(environment.cgroupDir()).doesNotExist();
        assertThat(environment.workDir()).doesNotExist();
    }
}
