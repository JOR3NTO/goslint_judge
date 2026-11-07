package co.uceva.contestsplatform.auth.dto;

import co.uceva.contestsplatform.auth.model.Role;
import jakarta.validation.constraints.NotNull;

public record ChangeRoleRequest(
        @NotNull(message = "El rol es obligatorio")
        Role role
) {
}
