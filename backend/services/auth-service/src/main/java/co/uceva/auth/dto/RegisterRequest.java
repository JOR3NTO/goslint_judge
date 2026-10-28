package co.uceva.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres")
        String firstName,

        @NotBlank(message = "El apellido es obligatorio")
        @Size(min = 2, max = 50, message = "El apellido debe tener entre 2 y 50 caracteres")
        String lastName,

        @NotBlank(message = "El nombre de usuario es obligatorio")
        @Pattern(regexp = ValidationRules.USERNAME_PATTERN, message = ValidationRules.USERNAME_MESSAGE)
        String username,

        @NotBlank(message = "El correo electrónico es obligatorio")
        @Email(message = "Debe proporcionar un correo electrónico válido")
        @Size(max = 255, message = "El correo no puede superar 255 caracteres")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        @Pattern(regexp = ValidationRules.PASSWORD_PATTERN, message = ValidationRules.PASSWORD_MESSAGE)
        String password,

        /** Institución a la que pertenece (opcional). */
        @Size(max = 255, message = "La institución no puede superar 255 caracteres")
        String institution
) {
}
