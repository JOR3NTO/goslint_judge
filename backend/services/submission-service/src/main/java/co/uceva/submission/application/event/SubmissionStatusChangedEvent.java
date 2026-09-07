package co.uceva.submission.application.event;

import co.uceva.submission.domain.model.Submission;

/**
 * Evento interno de la aplicación que señala que el estado de un envío acaba de
 * cambiar y debe notificarse a quien lo esté siguiendo.
 * <p>
 * No viaja por la red: igual que {@link SubmissionPersistedEvent}, se publica
 * dentro del propio proceso para que la notificación salga <em>después</em> de
 * que la transacción haya hecho commit. Así el estudiante nunca ve en pantalla
 * un veredicto que la base de datos terminó descartando.
 * </p>
 * <p>
 * Es también lo que mantiene el canal de notificación desacoplado del flujo de
 * evaluación: quien registra el veredicto solo publica este evento y no sabe si
 * al otro lado hay un WebSocket, un correo o nada en absoluto.
 * </p>
 *
 * @param submission Envío cuyo estado acaba de actualizarse.
 */
public record SubmissionStatusChangedEvent(Submission submission) {}
