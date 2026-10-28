package co.uceva.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateUsernameRequest(
        @NotBlank(message = "El nombre de usuario no puede estar vacío.")
        @Pattern(regexp = ValidationRules.USERNAME_PATTERN, message = ValidationRules.USERNAME_MESSAGE)
        String username
) {
}
