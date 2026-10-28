package co.uceva.auth.service;

import co.uceva.auth.cache.AuthCache;
import co.uceva.auth.exception.ForbiddenOperationException;
import co.uceva.auth.exception.UserNotFoundException;
import co.uceva.auth.model.Role;
import co.uceva.auth.model.User;
import co.uceva.auth.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/**
 * Administración de usuarios para el panel de administración.
 * <p>
 * Quién puede hacer qué (el rol se lee de la base de datos, no del JWT, para que un
 * administrador degradado pierda el acceso al instante):
 * </p>
 * <ul>
 *   <li>{@code ADMIN}: lista y busca usuarios, activa o desactiva cuentas y alterna el rol
 *       entre {@code STUDENT} y {@code ORGANIZER}. No toca a otros administradores.</li>
 *   <li>{@code SUPER_ADMIN}: además asigna y retira {@code ADMIN} y gestiona a los
 *       administradores.</li>
 *   <li>Nadie, por la API, modifica a un {@code SUPER_ADMIN}, asigna ese rol ni se modifica a
 *       sí mismo.</li>
 * </ul>
 */
@Service
public class AdminUserService {

    static final int MAX_PAGE_SIZE = 100;

    /** Roles que un {@code ADMIN} puede asignar y cuyas cuentas puede gestionar. */
    private static final Set<Role> MANAGEABLE_BY_ADMIN = EnumSet.of(Role.STUDENT, Role.ORGANIZER);

    private final UserRepository userRepository;
    private final ProfileService profileService;
    private final AuthCache authCache;

    public AdminUserService(UserRepository userRepository, ProfileService profileService, AuthCache authCache) {
        this.userRepository = userRepository;
        this.profileService = profileService;
        this.authCache = authCache;
    }

    /**
     * Página de usuarios, del más reciente al más antiguo.
     *
     * @param search Texto opcional; coincide parcialmente con username o correo, sin distinguir mayúsculas.
     * @param page   Número de página desde 0; un valor negativo se toma como 0.
     * @param size   Tamaño de página, limitado a {@value #MAX_PAGE_SIZE}.
     */
    public Page<User> listUsers(String rawToken, String search, int page, int size) {
        requireAdmin(rawToken);

        PageRequest pageable = PageRequest.of(
                Math.max(0, page),
                Math.min(Math.max(1, size), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        String term = search == null ? "" : search.trim();
        if (term.isEmpty()) {
            return userRepository.findAll(pageable);
        }
        return userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(term, term, pageable);
    }

    /**
     * Cambia el rol de un usuario. Cierra sus sesiones para que el nuevo rol rija en
     * cuanto expire su access token (máximo 15 minutos).
     */
    public User changeRole(String rawToken, UUID targetId, Role newRole) {
        User actor = requireAdmin(rawToken);
        User target = findTarget(targetId);

        if (newRole == Role.SUPER_ADMIN) {
            throw new ForbiddenOperationException("El rol de super administrador no se puede asignar.");
        }
        if (target.getId().equals(actor.getId())) {
            throw new ForbiddenOperationException("No puedes cambiar tu propio rol.");
        }
        requireCanManage(actor, target);
        if (actor.getRole() == Role.ADMIN && !MANAGEABLE_BY_ADMIN.contains(newRole)) {
            throw new ForbiddenOperationException("Solo el super administrador puede asignar el rol de administrador.");
        }

        if (target.getRole() == newRole) {
            return target;
        }
        target.setRole(newRole);
        User saved = userRepository.save(target);
        authCache.deleteRefreshToken(saved.getId().toString());
        return saved;
    }

    /**
     * Activa o desactiva una cuenta. Al desactivarla se cierra su sesión; el access token
     * ya emitido sigue siendo válido hasta que expire (máximo 15 minutos), pero ya no puede
     * renovarse ni consultar su perfil.
     */
    public User changeStatus(String rawToken, UUID targetId, boolean active) {
        User actor = requireAdmin(rawToken);
        User target = findTarget(targetId);

        if (!active && target.getId().equals(actor.getId())) {
            throw new ForbiddenOperationException("No puedes desactivar tu propia cuenta.");
        }
        requireCanManage(actor, target);

        if (target.isActive() == active) {
            return target;
        }
        target.setActive(active);
        User saved = userRepository.save(target);
        if (!active) {
            authCache.deleteRefreshToken(saved.getId().toString());
        }
        return saved;
    }

    /** Valida el token y que su dueño sea {@code ADMIN} o {@code SUPER_ADMIN} según la base de datos. */
    private User requireAdmin(String rawToken) {
        User actor = profileService.getCurrentUser(rawToken);
        if (actor.getRole() != Role.ADMIN && actor.getRole() != Role.SUPER_ADMIN) {
            throw new ForbiddenOperationException("Se requiere rol de administrador.");
        }
        return actor;
    }

    private User findTarget(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado."));
    }

    private void requireCanManage(User actor, User target) {
        if (target.getRole() == Role.SUPER_ADMIN) {
            throw new ForbiddenOperationException("La cuenta de super administrador no se puede modificar.");
        }
        if (actor.getRole() == Role.ADMIN && !MANAGEABLE_BY_ADMIN.contains(target.getRole())) {
            throw new ForbiddenOperationException("Solo el super administrador puede gestionar a los administradores.");
        }
    }
}
