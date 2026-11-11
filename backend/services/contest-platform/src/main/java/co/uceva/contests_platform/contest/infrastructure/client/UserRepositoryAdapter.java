package co.uceva.contests_platform.contest.infrastructure.client;

import java.util.Optional;
import java.util.UUID;

import co.uceva.contests_platform.auth.exception.UserNotFoundException;
import co.uceva.contests_platform.auth.service.GetUserByIdUseCase;
import org.springframework.stereotype.Component;

/**
 * Adaptador del módulo {@code contest} hacia el módulo {@code auth}: consulta los datos
 * del usuario llamando a su caso de uso, dentro del mismo proceso (sin HTTP).
 */
@Component
public class UserRepositoryAdapter {

    private final GetUserByIdUseCase getUserByIdUseCase;

    public UserRepositoryAdapter(GetUserByIdUseCase getUserByIdUseCase) {
        this.getUserByIdUseCase = getUserByIdUseCase;
    }

    /** Correo registrado del usuario, o vacío si no existe. */
    public Optional<String> findEmailByUserId(UUID userId) {
        try {
            return Optional.of(getUserByIdUseCase.execute(userId).getEmail());
        } catch (UserNotFoundException e) {
            return Optional.empty();
        }
    }
}
