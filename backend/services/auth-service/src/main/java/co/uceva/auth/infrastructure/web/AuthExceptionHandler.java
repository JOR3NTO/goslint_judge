package co.uceva.auth.infrastructure.web;

import co.uceva.auth.domain.exception.InvalidUserDataException;
import co.uceva.auth.domain.exception.UserAlreadyExistsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * Interceptor global de excepciones para el módulo de Autenticación.
 * Transforma las excepciones de Java en respuestas HTTP JSON estandarizadas
 * para que el frontend pueda consumirlas fácilmente.
 */
@ControllerAdvice
public class AuthExceptionHandler {

    /**
     * Maneja el caso en que un correo o username ya esté tomado (Excepción de negocio).
     * @return HTTP 409 Conflict con el mensaje de error.
     */
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        Map<String, String> response = new HashMap<>();
        response.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * Maneja los errores arrojados por las validaciones del DTO (@Valid).
     * Atrapa los fallos y extrae exactamente qué campo falló y por qué,
     * cumpliendo con el Criterio de Aceptación #4 de la historia de usuario.
     * 
     * @return HTTP 400 Bad Request con un mapa de (campo -> mensaje de error).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        // Itera sobre todos los errores capturados por Hibernate Validator
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }
    
    /**
     * Maneja errores genéricos de datos inválidos desde la capa de dominio.
     * @return HTTP 400 Bad Request.
     */
    @ExceptionHandler(InvalidUserDataException.class)
    public ResponseEntity<Map<String, String>> handleInvalidUserData(InvalidUserDataException ex) {
        Map<String, String> response = new HashMap<>();
        response.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}
