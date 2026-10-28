package co.uceva.contest.infrastructure.web.exception;

import co.uceva.contest.domain.exception.ExistingTeamException;
import co.uceva.contest.domain.exception.InvalidContestPasswordException;
import co.uceva.contest.domain.exception.InvalidInvitationException;
import co.uceva.contest.domain.exception.InvitationAccessDeniedException;
import co.uceva.contest.domain.exception.InvitationNotFoundException;
import co.uceva.contest.domain.exception.InvalidTeamException;
import co.uceva.contest.domain.exception.MaxTeamMembersReachedException;
import co.uceva.contest.domain.exception.MemberAlreadyInTeamException;
import co.uceva.contest.domain.exception.MemberNotFoundException;
import co.uceva.contest.domain.exception.RegistrationClosedException;
import co.uceva.contest.domain.exception.TeamNotFoundException;
import co.uceva.contest.infrastructure.web.dto.ErrorResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce las excepciones de dominio de equipos e integrantes en respuestas HTTP.
 */
@RestControllerAdvice
public class TeamExceptionHandler {

    @ExceptionHandler({TeamNotFoundException.class, MemberNotFoundException.class, InvitationNotFoundException.class})
    public ResponseEntity<ErrorResponseDTO> handleNotFound(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponseDTO(ex.getMessage()));
    }

    @ExceptionHandler({ExistingTeamException.class, MemberAlreadyInTeamException.class,
            MaxTeamMembersReachedException.class, InvalidInvitationException.class,
            RegistrationClosedException.class})
    public ResponseEntity<ErrorResponseDTO> handleConflict(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponseDTO(ex.getMessage()));
    }

    @ExceptionHandler(InvalidTeamException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidTeam(InvalidTeamException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponseDTO(ex.getMessage()));
    }

    @ExceptionHandler({InvalidContestPasswordException.class, InvitationAccessDeniedException.class})
    public ResponseEntity<ErrorResponseDTO> handleForbidden(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponseDTO(ex.getMessage()));
    }

    @ExceptionHandler(MailException.class)
    public ResponseEntity<ErrorResponseDTO> handleMail(MailException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponseDTO("No se pudo enviar el correo de invitación. Intenta de nuevo."));
    }
}
