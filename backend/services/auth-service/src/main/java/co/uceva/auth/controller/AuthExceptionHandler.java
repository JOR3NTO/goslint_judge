package co.uceva.auth.controller;

import co.uceva.auth.exception.AccountDisabledException;
import co.uceva.auth.exception.AccountLockedException;
import co.uceva.auth.exception.BadCredentialsException;
import co.uceva.auth.exception.ForbiddenOperationException;
import co.uceva.auth.exception.InvalidCurrentPasswordException;
import co.uceva.auth.exception.UserNotFoundException;
import co.uceva.auth.exception.InvalidUserDataException;
import co.uceva.auth.exception.UserAlreadyExistsException;
import co.uceva.auth.exception.UsernameTakenException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Traduce las excepciones del módulo a respuestas JSON {@code {"error": "..."}}.
 */
@RestControllerAdvice
public class AuthExceptionHandler {

    @ExceptionHandler({UserAlreadyExistsException.class, UsernameTakenException.class})
    public ResponseEntity<Map<String, String>> handleConflict(RuntimeException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * Dos peticiones simultáneas pasaron la comprobación previa y chocaron con el
     * UNIQUE de la base de datos.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrity(DataIntegrityViolationException ex) {
        return error(HttpStatus.CONFLICT, "El correo o el nombre de usuario ya está en uso.");
    }

    /** Contraseña actual incorrecta: 422 para distinguirla del 401 de token inválido. */
    @ExceptionHandler(InvalidCurrentPasswordException.class)
    public ResponseEntity<Map<String, String>> handleInvalidCurrentPassword(InvalidCurrentPasswordException ex) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    /** Falla de Bean Validation: devuelve {@code {campo: mensaje}}. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fieldError -> errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage()));
        ex.getBindingResult().getGlobalErrors()
                .forEach(globalError -> errors.putIfAbsent("error", globalError.getDefaultMessage()));
        return ResponseEntity.badRequest().body(errors);
    }

    /** Cuerpo ausente, JSON malformado o valor fuera de los permitidos (p. ej. un rol inexistente). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return error(HttpStatus.BAD_REQUEST, "El cuerpo de la petición no es un JSON válido o tiene un valor no permitido.");
    }

    @ExceptionHandler(InvalidUserDataException.class)
    public ResponseEntity<Map<String, String>> handleInvalidUserData(InvalidUserDataException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleBadCredentials(BadCredentialsException ex) {
        return error(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<Map<String, String>> handleAccountLocked(AccountLockedException ex) {
        return error(HttpStatus.LOCKED, ex.getMessage());
    }

    @ExceptionHandler(ForbiddenOperationException.class)
    public ResponseEntity<Map<String, String>> handleForbiddenOperation(ForbiddenOperationException ex) {
        return error(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleUserNotFound(UserNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /** UUID de ruta mal formado, p. ej. {@code /admin/users/abc/role}. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return error(HttpStatus.BAD_REQUEST, "El parámetro '" + ex.getName() + "' no es válido.");
    }

    @ExceptionHandler(AccountDisabledException.class)
    public ResponseEntity<Map<String, String>> handleAccountDisabled(AccountDisabledException ex) {
        return error(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    private static ResponseEntity<Map<String, String>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }
}
