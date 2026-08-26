package co.uceva.submission.application.usecase.impl;

import co.uceva.submission.application.port.out.SubmissionStatusNotifier;
import co.uceva.submission.application.port.out.TeamDTO;
import co.uceva.submission.application.port.out.TeamMembershipPort;
import co.uceva.submission.application.usecase.NotifySubmissionStatusUseCase;
import co.uceva.submission.domain.model.Submission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Servicio de Aplicación que decide a quién pertenece un envío y le empuja su
 * estado actual.
 * <p>
 * Aquí vive la regla que impide que nadie vea envíos ajenos: los destinatarios
 * se derivan del equipo dueño del envío y de ninguna otra fuente. En particular,
 * jamás se derivan de nada que el cliente haya podido decir por el canal de
 * notificación, que es de un solo sentido.
 * </p>
 */
@Service
public class NotifySubmissionStatusUseCaseImpl implements NotifySubmissionStatusUseCase {

    private static final Logger log = LoggerFactory.getLogger(NotifySubmissionStatusUseCaseImpl.class);

    private final TeamMembershipPort teamMembershipPort;
    private final SubmissionStatusNotifier submissionStatusNotifier;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param teamMembershipPort       Puerto de salida para conocer los integrantes de un equipo.
     * @param submissionStatusNotifier Puerto de salida para empujar el estado a los interesados.
     */
    public NotifySubmissionStatusUseCaseImpl(TeamMembershipPort teamMembershipPort,
            SubmissionStatusNotifier submissionStatusNotifier) {
        this.teamMembershipPort = teamMembershipPort;
        this.submissionStatusNotifier = submissionStatusNotifier;
    }

    /**
     * Resuelve los integrantes del equipo dueño del envío y les notifica el
     * estado actual.
     * <p>
     * Un equipo sin integrantes conocidos no es un error: significa que no hay a
     * quién avisar en este momento, y el estado sigue disponible por HTTP.
     * </p>
     *
     * @param submission Envío cuyo estado acaba de cambiar.
     */
    @Override
    public void execute(Submission submission) {
        TeamDTO team = teamMembershipPort.findTeam(submission.getTeamId());

        if (team.memberUserIds().isEmpty()) {
            log.debug("El equipo {} no tiene integrantes conocidos; el envío {} no se notifica.",
                    submission.getTeamId(), submission.getId());
            return;
        }

        submissionStatusNotifier.notifyStatusChanged(submission, team.memberUserIds());
    }
}
