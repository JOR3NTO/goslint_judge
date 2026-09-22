package co.uceva.auth.domain.repository;

import co.uceva.auth.domain.model.User;
import java.util.Optional;

/**
 * Puerto de Salida (Output Port) para el repositorio de Usuarios.
 * Aisla la capa de negocio de los detalles de cómo se guardan los datos en BD.
 */
public interface UserRepository {
    User save(User user);
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
}
