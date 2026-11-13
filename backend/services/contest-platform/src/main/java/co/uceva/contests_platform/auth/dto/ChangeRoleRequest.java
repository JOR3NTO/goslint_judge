package co.uceva.contests_platform.auth.dto;

import co.uceva.contests_platform.auth.model.Role;
import jakarta.validation.constraints.NotNull;

public record ChangeRoleRequest(
        @NotNull(message = "El rol es obligatorio")
        Role role
) {
}
