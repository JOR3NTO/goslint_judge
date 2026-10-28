package co.uceva.auth.dto;

import co.uceva.auth.model.User;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.UUID;

/** Un usuario tal como lo ve el panel de administración. Nunca incluye el hash de la contraseña. */
public record AdminUserResponse(
        UUID id,
        String firstName,
        String lastName,
        String username,
        String email,
        String role,
        String institution,
        boolean isActive,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime createdAt
) {

    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name(),
                user.getInstitution(),
                user.isActive(),
                user.getCreatedAt());
    }
}
