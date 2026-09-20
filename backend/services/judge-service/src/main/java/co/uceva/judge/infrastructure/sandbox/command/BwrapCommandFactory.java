package co.uceva.judge.infrastructure.sandbox.command;

import java.util.ArrayList;
import java.util.List;

import co.uceva.judge.domain.valueobject.VolumeSizeLimit;
import co.uceva.judge.infrastructure.sandbox.workspace.SandboxWorkspace;

/**
 * Construye el comando del sistema que ejecuta una solución dentro del
 * sandbox de {@code bubblewrap} (bwrap), aplicando aislamiento de namespaces,
 * montajes de solo lectura/escritura, el perfil de seccomp y la inscripción
 * del proceso en su cgroup.
 */
public final class BwrapCommandFactory {

    private BwrapCommandFactory() {}

    /**
     * Construye la lista de argumentos del comando completo listo para ser
     * lanzado mediante {@link ProcessBuilder}.
     *
     * @param workspace     Workspace (cgroup y directorio de trabajo) del intento de ejecución.
     * @param maxVolumeSize Tamaño máximo del volumen escribible ({@code /work}) dentro del sandbox.
     * @param command       Intérprete o comando usado para ejecutar la solución.
     * @return Lista de argumentos del comando completo.
     */
    public static List<String> build(SandboxWorkspace workspace, VolumeSizeLimit maxVolumeSize, String command) {

        List<String> runCommand = new ArrayList<>(List.of(
                "/bin/sh",
                "-c",
                "exec bwrap \"$@\" 3<\"$SECCOMP_PROFILE\"",
                "sh"
        ));

        List<String> argsDeBwrap = List.of(
                // Gestion del proceso
                "--die-with-parent", // Termina el proceso del sandbox si el proceso padre (el shell) muere.
                "--new-session", // Crea una nueva sesion de terminal, evitando que el proceso la controle.
                "--clearenv", // Limpia todas las variables de entorno heredadas del proceso padre.
                "--setenv", "PATH", "/usr/bin:/bin", // Define explicitamente el PATH disponible dentro del sandbox.

                // Namespaces
                "--unshare-user", // Aisla el proceso en su propio namespace de usuarios.
                "--unshare-ipc", // Aisla los mecanismos de comunicacion entre procesos (IPC).
                "--unshare-pid", // Aisla el arbol de procesos en su propio namespace de PIDs.
                "--unshare-net", // Aisla el acceso a la red, dejando al proceso sin conectividad.
                "--unshare-uts", // Aisla el hostname y el dominio del sistema (namespace UTS).

                // Sistema base
                "--ro-bind", "/usr", "/usr", // Monta /usr del host en modo solo lectura dentro del sandbox.
                "--symlink", "usr/bin", "/bin", // Crea el enlace simbolico /bin -> usr/bin.
                "--symlink", "usr/lib", "/lib", // Crea el enlace simbolico /lib -> usr/lib.
                "--symlink", "usr/lib64", "/lib64", // Crea el enlace simbolico /lib64 -> usr/lib64.

                "--proc", "/proc", // Monta un /proc nuevo y aislado para el sandbox.
                "--dev", "/dev", // Monta un /dev minimo dentro del sandbox.

                // Codigo fuente: solo lectura
                "--ro-bind", workspace.workDir(), "/solution", // Monta el directorio con el codigo fuente en modo solo lectura.

                // Directorio de trabajo: escribible
                "--size", String.valueOf(maxVolumeSize.bytes()), // Limita el tamano del tmpfs escribible que se monta a continuacion.
                "--tmpfs", "/work", // Monta un tmpfs escribible y limitado en tamano como directorio de trabajo.

                // Temporales adicionales
                "--tmpfs", "/tmp", // Monta un tmpfs adicional para archivos temporales.

                "--chdir", "/work", // Establece /work como directorio de trabajo actual dentro del sandbox.

                // Acceso al cgroup del programa
                "--bind", workspace.cgroupProcs().toString(), "/run/cgp", // Monta el archivo cgroup.procs del cgroup para poder inscribir el proceso.

                // Seccomp
                "--seccomp", "3", // Aplica el filtro seccomp leido desde el descriptor de archivo 3.

                // Comando dentro del sandbox
                "--", // Marca el fin de las opciones de bwrap y el inicio del comando a ejecutar.
                "/bin/sh", "-c",
                "echo $$ > /run/cgp; exec \"$@\"", // Inscribe el proceso en el cgroup y luego ejecuta el comando real.
                "--",
                command, // Interprete o comando con el que se ejecuta la solucion.
                "/solution/" + workspace.origen().getFileName().toString() // Ruta, dentro del sandbox, del archivo de la solucion.
        );

        runCommand.addAll(argsDeBwrap);
        return runCommand;
    }
}
