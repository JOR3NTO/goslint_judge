package co.uceva.judge.domain.exception;

/**
 * Excepción de dominio lanzada cuando el sandbox falla por un problema del
 * propio sistema (cgroup, bwrap, E/S) y no por el código del estudiante.
 * <p>
 * Se distingue de un veredicto como {@code RUNTIME_ERROR}: aquel culpa a la
 * solución, esta indica que la plataforma no pudo evaluarla.
 * </p>
 */
public class SandboxExecutionException extends RuntimeException {

    /**
     * Construye la excepción con un mensaje y la causa original.
     *
     * @param message Descripción del fallo.
     * @param cause   Causa original del fallo.
     */
    public SandboxExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
