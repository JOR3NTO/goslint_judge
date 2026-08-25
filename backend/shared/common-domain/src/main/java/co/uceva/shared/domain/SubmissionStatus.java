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
    JUDGED
}
