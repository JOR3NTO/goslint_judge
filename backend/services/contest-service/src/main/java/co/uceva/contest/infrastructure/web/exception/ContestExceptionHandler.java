package co.uceva.contest.infrastructure.web.exception;

import co.uceva.contest.domain.exception.ContestNotFoundException;
import co.uceva.contest.domain.exception.ExistingContestException;
import co.uceva.contest.infrastructure.web.dto.ErrorResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Interceptor global de excepciones para el módulo de Concursos.
 * Traduce las excepciones de dominio en respuestas HTTP con mensajes
 * de error claros y consistentes para el cliente.
 */
@RestControllerAdvice
public class ContestExceptionHandler {

    @ExceptionHandler(ContestNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleContestNotFound(ContestNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponseDTO(ex.getMessage()));
    }

    @ExceptionHandler(ExistingContestException.class)
    public ResponseEntity<ErrorResponseDTO> handleExistingContest(ExistingContestException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponseDTO(ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDTO> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponseDTO(ex.getMessage()));
    }
}
