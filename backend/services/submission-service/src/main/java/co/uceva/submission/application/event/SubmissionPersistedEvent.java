package co.uceva.submission.application.event;

import co.uceva.submission.domain.model.Submission;

/**
 * Evento interno de la aplicación que señala que un envío ya fue persistido y
 * está listo para ser entregado al motor de evaluación.
 * <p>
 * No viaja por la red: se publica dentro del propio proceso para que el
 * encolamiento ocurra <em>después</em> de que la transacción haya hecho commit.
 * Así se evita el escenario en el que el juez recibe un envío que la base de
 * datos terminó descartando, y se garantiza que un fallo del broker no deshaga
 * un envío ya aceptado.
 * </p>
 *
 * @param submission Envío recién persistido.
 */
public record SubmissionPersistedEvent(Submission submission) {}
