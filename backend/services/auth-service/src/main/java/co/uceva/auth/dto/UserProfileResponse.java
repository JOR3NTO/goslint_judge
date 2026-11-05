package co.uceva.auth.dto;

import co.uceva.auth.model.User;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Respuesta de {@code GET /me} y {@code PATCH /me/username}. El nombre del
 * campo {@code isActive} es el que espera el frontend (ProfileService).
 */
public record UserProfileResponse(
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

    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
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
