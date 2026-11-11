package co.uceva.contests_platform.auth.repository;

import co.uceva.contests_platform.auth.model.Role;
import co.uceva.contests_platform.auth.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCase(String username);

    /** Igual que {@link #existsByUsernameIgnoreCase} pero ignorando al propio usuario. */
    boolean existsByUsernameIgnoreCaseAndIdNot(String username, UUID id);

    boolean existsByRole(Role role);

    /** Busca por coincidencia parcial en username o correo. Spring Data escapa {@code %} y {@code _}. */
    Page<User> findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String username, String email, Pageable pageable);

    /** Usuarios activos cuyo username contiene el texto (sin distinguir mayúsculas), excluyendo a {@code excludedId}. */
    List<User> findByActiveTrueAndIdNotAndUsernameContainingIgnoreCase(
            UUID excludedId, String username, Pageable pageable);
}
