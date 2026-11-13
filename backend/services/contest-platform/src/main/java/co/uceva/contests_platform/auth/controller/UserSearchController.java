package co.uceva.contests_platform.auth.controller;

import java.util.List;

import co.uceva.contests_platform.auth.dto.UserSummaryResponse;
import co.uceva.contests_platform.auth.service.SearchUsersByUsernameUseCase;
import co.uceva.shared.infrastructure.security.AuthenticatedUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Búsqueda de usuarios por username para cualquier usuario autenticado. */
@RestController
@RequestMapping("/api/v1/users")
@PreAuthorize("isAuthenticated()")
public class UserSearchController {

    private final SearchUsersByUsernameUseCase searchUsers;

    public UserSearchController(SearchUsersByUsernameUseCase searchUsers) {
        this.searchUsers = searchUsers;
    }

    /** Hasta 20 usuarios activos cuyo username contiene {@code q}; excluye al usuario autenticado. */
    @GetMapping("/search")
    public ResponseEntity<List<UserSummaryResponse>> search(
            @RequestParam(name = "q", defaultValue = "") String q,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(searchUsers.execute(q, user.userId()).stream()
                .map(UserSummaryResponse::from).toList());
    }
}
