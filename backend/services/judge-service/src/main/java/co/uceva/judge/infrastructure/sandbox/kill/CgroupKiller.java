package co.uceva.judge.infrastructure.sandbox.kill;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class CgroupKiller {
    private final Path cgLeafPath;

    public CgroupKiller(Path cgLeafPath) {
        this.cgLeafPath = cgLeafPath;
    }

    /**
     * Escribe en {@code cgroup.kill} para forzar la terminación de todos los
     * procesos pertenecientes al cgroup del proceso monitoreado.
     */
    public void killCgroup() {
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
}
