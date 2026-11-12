package co.uceva.contests_platform.auth.service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import co.uceva.contests_platform.auth.model.Role;
import co.uceva.contests_platform.auth.model.User;
import co.uceva.contests_platform.auth.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/** Caso de uso: busca usuarios activos por username (p. ej. para elegir a quién invitar a un equipo). */
@Service
public class SearchUsersByUsernameUseCase {

    static final int MAX_RESULTS = 20;

    /** Administradores y organizadores no aparecen en la búsqueda: no son candidatos a invitar a un equipo. */
    static final Set<Role> EXCLUDED_ROLES = Set.of(Role.ADMIN, Role.SUPER_ADMIN, Role.ORGANIZER);

    private final UserRepository userRepository;

    public SearchUsersByUsernameUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * @param term       Texto que debe contener el username; vacío devuelve los primeros por orden alfabético.
     * @param excludedId Usuario que se omite del resultado (quien hace la búsqueda).
     */
    public List<User> execute(String term, UUID excludedId) {
        String text = term == null ? "" : term.trim();
        return userRepository.findByActiveTrueAndRoleNotInAndIdNotAndUsernameContainingIgnoreCase(
                EXCLUDED_ROLES, excludedId, text, PageRequest.of(0, MAX_RESULTS, Sort.by("username").ascending()));
    }
}
