package co.uceva.contestsplatform.contestmodule.application.usecase;

import java.util.List;
import java.util.UUID;

import co.uceva.contestsplatform.contestmodule.domain.model.Invitation;

/** Puerto de entrada: consulta las invitaciones recibidas por un usuario. */
public interface GetInvitationsByUserUseCase {

    List<Invitation> execute(UUID userId);
}
