package co.uceva.shared.domain;

/**
 * Enumeración que representa los posibles veredictos de evaluación de un envío
 * en el sistema de juez en línea.
 * <p>
 * Un veredicto describe el resultado final obtenido tras ejecutar el código
 * fuente del estudiante contra los casos de prueba de un problema.
 * </p>
 */
public enum VerdictStatus {
    /** Envío recibido y en espera de ser evaluado. */
    PENDING,
    /** La solución produjo la salida esperada en todos los casos de prueba. */
    ACCEPTED,
    /** La solución produjo una salida diferente a la esperada en algún caso. */
    WRONG_ANSWER,
    /** La solución excedió el límite de tiempo permitido. */
    TIME_LIMIT_EXCEEDED,
    /** La solución excedió el límite de memoria permitido. */
    MEMORY_LIMIT_EXCEEDED,
    /** La solución finalizó con un error en tiempo de ejecución. */
    RUNTIME_ERROR,
    /** La solución no pudo compilarse correctamente. */
    COMPILATION_ERROR
}
