package co.uceva.judge.infrastructure.web.handler;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduce los errores de validación de las peticiones a respuestas {@code 400}. */
@RestControllerAdvice
public class JudgeExceptionHandler {

    /**
     * Un valor fuera del rango que permite su value object.
     *
     * @param e Excepción de dominio.
     * @return {@code 400} con el motivo.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleInvalidValue(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
    }

    /**
     * Un campo obligatorio ausente en el cuerpo.
     *
     * @param e Excepción de validación.
     * @return {@code 400} con los campos inválidos.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleMissingField(MethodArgumentNotValidException e) {
        String fields = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField()).sorted().reduce((a, b) -> a + ", " + b).orElse("");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Campos obligatorios: " + fields));
    }
}
