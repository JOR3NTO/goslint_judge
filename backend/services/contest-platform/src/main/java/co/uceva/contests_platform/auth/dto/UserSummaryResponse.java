package co.uceva.contests_platform.auth.dto;

import java.util.UUID;

import co.uceva.contests_platform.auth.model.User;

/** Datos mínimos y públicos de un usuario para listas de selección. No expone correo ni rol. */
public record UserSummaryResponse(UUID id, String username) {

    public static UserSummaryResponse from(User user) {
        return new UserSummaryResponse(user.getId(), user.getUsername());
    }
}
