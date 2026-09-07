package co.uceva.shared.domain;

/**
 * Enumeración que representa el estado del ciclo de vida de un envío dentro del
 * flujo de evaluación, independientemente del veredicto obtenido.
 * <p>
 * Es ortogonal a {@link VerdictStatus}: mientras el juez trabaja, el estado
 * habitual de un envío es {@code status = QUEUED} con {@code verdict = PENDING}.
 * "En cola" describe dónde está el envío, no cómo resultó evaluado.
 * </p>
 */
public enum SubmissionStatus {
    /** Envío persistido pero aún no entregado al motor de evaluación. */
    PENDING,
    /** Entrega al motor de evaluación confirmada por el broker de mensajería. */
    QUEUED,
    /** El motor de evaluación tomó el envío y lo está procesando. */
    JUDGING,
    /** El motor de evaluación finalizó y emitió un veredicto. */
    JUDGED,
    /**
     * El envío no pudo evaluarse por un fallo del propio sistema, agotados todos
     * los reintentos.
     * <p>
     * Es un estado terminal y excepcional: no dice nada sobre la corrección del
     * código del estudiante (su {@code verdict} sigue siendo {@code PENDING}),
     * sino que la plataforma no consiguió emitir un veredicto. Se distingue de
     * {@code JUDGING} o {@code QUEUED} precisamente para que un envío averiado no
     * se quede indefinidamente aparentando estar en curso.
     * </p>
     */
    SYSTEM_ERROR
}
