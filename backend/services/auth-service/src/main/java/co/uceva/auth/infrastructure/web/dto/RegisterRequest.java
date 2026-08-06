package co.uceva.auth.infrastructure.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Data Transfer Object (DTO) que mapea la petición JSON del cliente.
 * Contiene validaciones integradas de Jakarta Validation para rechazar
 * peticiones mal formadas antes de que lleguen a la capa de negocio.
 */
@Data
public class RegisterRequest {
    /** Nombre de usuario. Es obligatorio y debe tener entre 3 y 50 caracteres. */
    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(min = 3, max = 50, message = "El nombre de usuario debe tener entre 3 y 50 caracteres")
    private String username;

    /** Correo electrónico. Es obligatorio y debe tener un formato válido (ej. x@y.com). */
    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "Debe proporcionar un correo electrónico válido")
    private String email;

    /** Contraseña proporcionada por el usuario (en texto plano en la petición). */
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    private String password;

    /** Nombre de la institución a la que pertenece (opcional). */
    private String institution;
}
