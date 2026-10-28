package co.uceva.contest.infrastructure.web.exception;

import co.uceva.contest.domain.exception.ClarificationAlreadyAnsweredException;
import co.uceva.contest.domain.exception.ClarificationNotFoundException;
import co.uceva.contest.domain.exception.InvalidClarificationException;
import co.uceva.contest.infrastructure.web.dto.ErrorResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce las excepciones de dominio de las aclaraciones en respuestas HTTP.
 */
@RestControllerAdvice
public class ClarificationExceptionHandler {

    @ExceptionHandler(ClarificationNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleNotFound(ClarificationNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponseDTO(ex.getMessage()));
    }

    @ExceptionHandler(ClarificationAlreadyAnsweredException.class)
    public ResponseEntity<ErrorResponseDTO> handleConflict(ClarificationAlreadyAnsweredException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponseDTO(ex.getMessage()));
    }

    @ExceptionHandler(InvalidClarificationException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalid(InvalidClarificationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponseDTO(ex.getMessage()));
    }
}
