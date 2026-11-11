package co.uceva.contests_platform.auth.service;

import java.util.UUID;

import co.uceva.contests_platform.auth.exception.UserNotFoundException;
import co.uceva.contests_platform.auth.model.User;
import co.uceva.contests_platform.auth.repository.UserRepository;
import org.springframework.stereotype.Service;

/** Caso de uso: consulta un usuario por su id. Lo consumen otros módulos (p. ej. {@code contest}). */
@Service
public class GetUserByIdUseCase {

    private final UserRepository userRepository;

    public GetUserByIdUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** @throws UserNotFoundException si no existe un usuario con ese id. */
    public User execute(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("No existe el usuario con id " + userId + "."));
    }
}
