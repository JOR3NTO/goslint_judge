package co.uceva.problem.infrastructure.web.exception;

import co.uceva.problem.domain.exception.ExistingProblemException;
import co.uceva.problem.domain.exception.ExistingTestCaseException;
import co.uceva.problem.domain.exception.NoProblemsException;
import co.uceva.problem.domain.exception.NoTestCasesException;
import co.uceva.problem.domain.exception.ProblemNotFoundException;
import co.uceva.problem.domain.exception.TestCaseNotFoundException;
import co.uceva.problem.infrastructure.web.dto.ErrorResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce las excepciones de dominio de problemas y casos de prueba en respuestas HTTP.
 * Sin este traductor, un problema inexistente respondía 500 y los clientes no podían
 * distinguirlo de un fallo real del servicio.
 */
@RestControllerAdvice
public class ProblemExceptionHandler {

    @ExceptionHandler({ProblemNotFoundException.class, TestCaseNotFoundException.class,
            NoProblemsException.class, NoTestCasesException.class})
    public ResponseEntity<ErrorResponseDTO> handleNotFound(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponseDTO(ex.getMessage()));
    }

    @ExceptionHandler({ExistingProblemException.class, ExistingTestCaseException.class})
    public ResponseEntity<ErrorResponseDTO> handleConflict(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponseDTO(ex.getMessage()));
    }
}
