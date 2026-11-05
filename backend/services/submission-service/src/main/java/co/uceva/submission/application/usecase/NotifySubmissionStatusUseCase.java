package co.uceva.submission.application.usecase;

import co.uceva.submission.domain.model.Submission;

/**
 * Puerto de entrada para hacer llegar a los interesados el estado actual de un
 * envío.
 * <p>
 * Concentra la única decisión de la que depende que nadie vea envíos ajenos:
 * quién es "el interesado". Un envío se registra a nombre de un equipo, así que
 * los destinatarios son los integrantes de ese equipo y nadie más.
 * </p>
 */
public interface NotifySubmissionStatusUseCase {

    /**
     * Resuelve los destinatarios del envío y les empuja su estado actual.
     *
     * @param submission Envío cuyo estado acaba de cambiar.
     */
    void execute(Submission submission);
}
