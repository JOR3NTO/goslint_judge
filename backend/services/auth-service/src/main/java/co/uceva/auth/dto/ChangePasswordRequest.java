package co.uceva.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChangePasswordRequest(
        @NotBlank(message = "La contraseña actual no puede estar vacía.")
        String currentPassword,

        @NotBlank(message = "La nueva contraseña no puede estar vacía.")
        @Pattern(regexp = ValidationRules.PASSWORD_PATTERN, message = ValidationRules.PASSWORD_MESSAGE)
        String newPassword
) {
}
