package co.uceva.contests_platform.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import co.uceva.contests_platform.auth.model.Role;
import co.uceva.contests_platform.auth.model.User;
import co.uceva.contests_platform.auth.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

@DisplayName("Búsqueda de usuarios por username")
class SearchUsersByUsernameUseCaseTest {

    private final UserRepository users = mock(UserRepository.class);
    private final SearchUsersByUsernameUseCase useCase = new SearchUsersByUsernameUseCase(users);

    @Test
    @DisplayName("Recorta el texto, excluye a quien busca y limita a 20 resultados")
    void trimsExcludesSelfAndLimits() {
        UUID me = UUID.randomUUID();
        User ana = User.builder().id(UUID.randomUUID()).username("ana_gomez").build();
        when(users.findByActiveTrueAndRoleNotInAndIdNotAndUsernameContainingIgnoreCase(
                eq(SearchUsersByUsernameUseCase.EXCLUDED_ROLES), eq(me), eq("ana"), any(Pageable.class)))
                .thenReturn(List.of(ana));

        List<User> result = useCase.execute("  ana ", me);

        assertThat(result).containsExactly(ana);
        verify(users).findByActiveTrueAndRoleNotInAndIdNotAndUsernameContainingIgnoreCase(
                eq(SearchUsersByUsernameUseCase.EXCLUDED_ROLES), eq(me), eq("ana"),
                org.mockito.ArgumentMatchers.argThat(p -> p.getPageSize() == 20));
    }

    @Test
    @DisplayName("Un texto nulo se trata como vacío")
    void nullTermIsEmpty() {
        UUID me = UUID.randomUUID();
        when(users.findByActiveTrueAndRoleNotInAndIdNotAndUsernameContainingIgnoreCase(
                eq(SearchUsersByUsernameUseCase.EXCLUDED_ROLES), eq(me), eq(""), any(Pageable.class)))
                .thenReturn(List.of());

        assertThat(useCase.execute(null, me)).isEmpty();
    }

    @Test
    @DisplayName("Excluye a administradores, super administradores y organizadores de la búsqueda")
    void excludesAdminAndOrganizerRoles() {
        assertThat(SearchUsersByUsernameUseCase.EXCLUDED_ROLES)
                .containsExactlyInAnyOrder(Role.ADMIN, Role.SUPER_ADMIN, Role.ORGANIZER);
    }
}
