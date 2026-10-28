package co.uceva.auth.dto;

import co.uceva.auth.model.Role;
import jakarta.validation.constraints.NotNull;

public record ChangeRoleRequest(
        @NotNull(message = "El rol es obligatorio")
        Role role
) {
}
