package co.uceva.contest.infrastructure.web.exception;

import co.uceva.contest.domain.exception.ContestProblemNotFoundException;
import co.uceva.contest.domain.exception.ExistingContestProblemException;
import co.uceva.contest.domain.exception.InvalidContestProblemException;
import co.uceva.contest.domain.exception.ProblemCatalogUnavailableException;
import co.uceva.contest.domain.exception.ProblemNotFoundException;
import co.uceva.contest.infrastructure.web.dto.ErrorResponseDTO;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce las excepciones de dominio de los problemas de un concurso en respuestas HTTP.
 */
@RestControllerAdvice
public class ContestProblemExceptionHandler {

    @ExceptionHandler({ContestProblemNotFoundException.class, ProblemNotFoundException.class})
    public ResponseEntity<ErrorResponseDTO> handleNotFound(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponseDTO(ex.getMessage()));
    }

    @ExceptionHandler(ExistingContestProblemException.class)
    public ResponseEntity<ErrorResponseDTO> handleConflict(ExistingContestProblemException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponseDTO(ex.getMessage()));
    }

    /** Respaldo de las restricciones UNIQUE ante dos peticiones simultáneas que pasaron la comprobación. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleIntegrity(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponseDTO("La operación viola una restricción de unicidad o integridad."));
    }

    @ExceptionHandler(InvalidContestProblemException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalid(InvalidContestProblemException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponseDTO(ex.getMessage()));
    }

    @ExceptionHandler(ProblemCatalogUnavailableException.class)
    public ResponseEntity<ErrorResponseDTO> handleCatalogUnavailable(ProblemCatalogUnavailableException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponseDTO("No se pudo verificar el problema: problem-service no está disponible."));
    }
}
