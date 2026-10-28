package co.uceva.contest.infrastructure.web.controller;

import java.util.List;
import java.util.UUID;

import co.uceva.contest.application.usecase.AcceptInvitationUseCase;
import co.uceva.contest.application.usecase.GetInvitationsByUserUseCase;
import co.uceva.contest.application.usecase.InviteMemberUseCase;
import co.uceva.contest.application.usecase.RejectInvitationUseCase;
import co.uceva.contest.infrastructure.web.dto.InvitationResponseDTO;
import co.uceva.contest.infrastructure.web.dto.InviteMemberRequestDTO;
import co.uceva.contest.infrastructure.web.dto.TeamResponseDTO;
import co.uceva.contest.infrastructure.web.mapper.TeamWebMapper;
import co.uceva.shared.infrastructure.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST de invitaciones a equipos. La identidad (capitán o invitado)
 * siempre sale del JWT, nunca del cuerpo de la petición.
 */
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/v1")
@PreAuthorize("isAuthenticated()")
public class InvitationController {

    private final InviteMemberUseCase inviteMemberUseCase;
    private final AcceptInvitationUseCase acceptInvitationUseCase;
    private final RejectInvitationUseCase rejectInvitationUseCase;
    private final GetInvitationsByUserUseCase getInvitationsByUserUseCase;

    public InvitationController(
            InviteMemberUseCase inviteMemberUseCase,
            AcceptInvitationUseCase acceptInvitationUseCase,
            RejectInvitationUseCase rejectInvitationUseCase,
            GetInvitationsByUserUseCase getInvitationsByUserUseCase) {
        this.inviteMemberUseCase = inviteMemberUseCase;
        this.acceptInvitationUseCase = acceptInvitationUseCase;
        this.rejectInvitationUseCase = rejectInvitationUseCase;
        this.getInvitationsByUserUseCase = getInvitationsByUserUseCase;
    }

    /** El capitán invita a un estudiante y se le envía el correo. 201 CREATED. */
    @PostMapping("/teams/{teamId}/invitations")
    public ResponseEntity<InvitationResponseDTO> invite(
            @PathVariable("teamId") UUID teamId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody InviteMemberRequestDTO request) {
        var invitation = inviteMemberUseCase.execute(TeamWebMapper.toCommand(teamId, user.userId(), request));
        return ResponseEntity.status(HttpStatus.CREATED).body(TeamWebMapper.toResponse(invitation));
    }

    /** Invitaciones recibidas por el usuario autenticado. */
    @GetMapping("/invitations/me")
    public ResponseEntity<List<InvitationResponseDTO>> getMine(@AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(getInvitationsByUserUseCase.execute(user.userId()).stream()
                .map(TeamWebMapper::toResponse).toList());
    }

    /** El invitado acepta y queda como miembro; devuelve el equipo actualizado. */
    @PostMapping("/invitations/{id}/accept")
    public ResponseEntity<TeamResponseDTO> accept(
            @PathVariable("id") UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(TeamWebMapper.toResponse(acceptInvitationUseCase.execute(id, user.userId())));
    }

    /** El invitado rechaza la invitación. 204 NO CONTENT. */
    @PostMapping("/invitations/{id}/reject")
    public ResponseEntity<Void> reject(
            @PathVariable("id") UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        rejectInvitationUseCase.execute(id, user.userId());
        return ResponseEntity.noContent().build();
    }
}
