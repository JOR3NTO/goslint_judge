package co.uceva.contest.infrastructure.web.controller;

import java.util.UUID;

import co.uceva.contest.application.usecase.AcceptInvitationUseCase;
import co.uceva.contest.application.usecase.GetInvitationByIdUseCase;
import co.uceva.contest.application.usecase.RejectInvitationUseCase;
import co.uceva.contest.domain.exception.ContestNotFoundException;
import co.uceva.contest.domain.exception.InvalidInvitationException;
import co.uceva.contest.domain.exception.InvitationNotFoundException;
import co.uceva.contest.domain.exception.MemberAlreadyInTeamException;
import co.uceva.contest.domain.exception.RegistrationClosedException;
import co.uceva.contest.domain.exception.TeamNotFoundException;
import co.uceva.contest.domain.model.Invitation;
import co.uceva.contest.domain.model.Team;
import co.uceva.contest.infrastructure.email.InvitationLinks;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;

/**
 * Atiende los botones del correo de invitación. No usa JWT: la autorización es el token
 * firmado del enlace. El GET solo muestra una confirmación y la acción ocurre en el POST,
 * para que los escáneres de enlaces de los clientes de correo no respondan por el usuario.
 */
@RestController
@RequestMapping("/api/v1/invitations/{id}/respond")
public class InvitationEmailController {

    private final GetInvitationByIdUseCase getInvitationByIdUseCase;
    private final AcceptInvitationUseCase acceptInvitationUseCase;
    private final RejectInvitationUseCase rejectInvitationUseCase;
    private final InvitationLinks links;
    private final String frontendUrl;

    public InvitationEmailController(GetInvitationByIdUseCase getInvitationByIdUseCase,
                                     AcceptInvitationUseCase acceptInvitationUseCase,
                                     RejectInvitationUseCase rejectInvitationUseCase,
                                     InvitationLinks links,
                                     @Value("${app.frontend-url}") String frontendUrl) {
        this.getInvitationByIdUseCase = getInvitationByIdUseCase;
        this.acceptInvitationUseCase = acceptInvitationUseCase;
        this.rejectInvitationUseCase = rejectInvitationUseCase;
        this.links = links;
        this.frontendUrl = frontendUrl;
    }

    @GetMapping(produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> confirm(@PathVariable("id") UUID id,
                                          @RequestParam("action") String action,
                                          @RequestParam("token") String token) {
        if (!isValidRequest(id, action, token)) {
            return invalidLink();
        }
        Invitation invitation;
        try {
            invitation = getInvitationByIdUseCase.execute(id);
        } catch (InvitationNotFoundException e) {
            return page(HttpStatus.NOT_FOUND, "Invitación no encontrada", HtmlUtils.htmlEscape(e.getMessage()), goToApp());
        }
        if (!invitation.isPending()) {
            return page(HttpStatus.CONFLICT, "Esta invitación ya fue respondida", alreadyResponded(invitation), goToApp());
        }
        boolean accept = action.equals("accept");
        // Sin campos ocultos: el form se envía a esta misma URL y action/token llegan solo por la query.
        String form = """
                <form method="post" style="margin:0;">
                  <button type="submit" style="%s">%s</button>
                </form>""".formatted(accept ? PRIMARY_BUTTON : SECONDARY_BUTTON,
                accept ? "Sí, unirme al equipo" : "Sí, rechazar");
        return page(HttpStatus.OK, accept ? "¿Aceptar la invitación?" : "¿Rechazar la invitación?",
                accept ? "Al confirmar quedarás como integrante del equipo."
                       : "El capitán podrá volver a invitarte si cambias de opinión.",
                form);
    }

    @PostMapping(produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> respond(@PathVariable("id") UUID id,
                                          @RequestParam("action") String action,
                                          @RequestParam("token") String token) {
        if (!isValidRequest(id, action, token)) {
            return invalidLink();
        }
        try {
            UUID userId = getInvitationByIdUseCase.execute(id).getInvitedUserId();
            if (action.equals("accept")) {
                var team = acceptInvitationUseCase.execute(id, userId);
                return page(HttpStatus.OK, "¡Ya eres parte del equipo!",
                        "Te uniste a <strong style=\"color:#45cd55;\">" + HtmlUtils.htmlEscape(team.getTeamName().value())
                                + "</strong>. Nos vemos en la maratón.", goToApp());
            }
            rejectInvitationUseCase.execute(id, userId);
            return page(HttpStatus.OK, "Invitación rechazada", "No te uniste al equipo.", goToApp());
        } catch (InvalidInvitationException | RegistrationClosedException | MemberAlreadyInTeamException
                 | InvitationNotFoundException | TeamNotFoundException | ContestNotFoundException e) {
            return page(HttpStatus.CONFLICT, "No se pudo completar", HtmlUtils.htmlEscape(e.getMessage()), goToApp());
        }
    }

    private static String alreadyResponded(Invitation invitation) {
        return switch (invitation.getStatus()) {
            case ACCEPTED -> "Ya aceptaste esta invitación: eres parte del equipo.";
            case REJECTED -> "Ya rechazaste esta invitación. Si cambias de opinión, pídele al capitán que te invite de nuevo.";
            case EXPIRED -> "La invitación ya no es válida: el equipo alcanzó el máximo de " + Team.DEFAULT_MAX_MEMBERS + " integrantes.";
            case PENDING -> "";
        };
    }

    private boolean isValidRequest(UUID id, String action, String token) {
        return (action.equals("accept") || action.equals("reject")) && links.isValid(id, token);
    }

    private ResponseEntity<String> invalidLink() {
        return page(HttpStatus.BAD_REQUEST, "Enlace no válido",
                "El enlace está incompleto o fue modificado. Ábrelo de nuevo desde el correo.", goToApp());
    }

    private String goToApp() {
        return "<a href=\"" + HtmlUtils.htmlEscape(frontendUrl) + "\" style=\"" + SECONDARY_BUTTON
                + "display:inline-block;text-decoration:none;\">Ir a Goslint Judge</a>";
    }

    private static final String PRIMARY_BUTTON = "cursor:pointer;border:0;border-radius:10px;background:#45cd55;"
            + "color:#020405;padding:13px 28px;font:700 15px Geist,'Segoe UI',Helvetica,Arial,sans-serif;";
    private static final String SECONDARY_BUTTON = "cursor:pointer;border:1px solid #19232a;border-radius:10px;"
            + "background:#0e1216;color:#eeeeee;padding:12px 24px;font:600 15px Geist,'Segoe UI',Helvetica,Arial,sans-serif;";

    private static ResponseEntity<String> page(HttpStatus status, String title, String message, String action) {
        String html = """
                <!doctype html>
                <html lang="es"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
                <title>%s · Goslint Judge</title></head>
                <body style="margin:0;min-height:100vh;background:#020405;color:#eeeeee;font-family:Geist,'Segoe UI',Helvetica,Arial,sans-serif;display:flex;align-items:center;justify-content:center;padding:16px;box-sizing:border-box;">
                  <main style="width:100%%;max-width:460px;">
                    <div style="display:flex;align-items:center;gap:10px;margin-bottom:20px;">
                      <span style="display:inline-flex;align-items:center;justify-content:center;width:36px;height:36px;border:1px solid #1f5a28;border-radius:8px;background:#0b1a0e;color:#45cd55;font:700 14px 'Geist Mono',Consolas,monospace;">&lt;/&gt;</span>
                      <span style="font-size:20px;font-weight:700;">Goslint<span style="color:#45cd55;">Judge</span></span>
                    </div>
                    <section style="background:#05080b;border:1px solid #19232a;border-radius:12px;padding:32px 28px;">
                      <h1 style="margin:0 0 12px 0;font-size:24px;line-height:1.3;">%s</h1>
                      <p style="margin:0 0 24px 0;font-size:15px;line-height:1.6;color:#a8a8a8;">%s</p>
                      %s
                    </section>
                  </main>
                </body></html>
                """.formatted(title, title, message, action);
        return ResponseEntity.status(status).contentType(MediaType.TEXT_HTML).body(html);
    }
}
