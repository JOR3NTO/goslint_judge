package co.uceva.auth.controller;

import co.uceva.auth.dto.AdminUserResponse;
import co.uceva.auth.dto.ChangeRoleRequest;
import co.uceva.auth.dto.ChangeStatusRequest;
import co.uceva.auth.dto.PageResponse;
import co.uceva.auth.service.AdminUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Administración de usuarios. Cada endpoint exige un access token de {@code ADMIN} o
 * {@code SUPER_ADMIN}: 401 si el token no sirve, 403 si su dueño no es administrador o la
 * operación rompe una regla de jerarquía (ver {@link AdminUserService}).
 */
@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    /** Página de usuarios; {@code search} filtra por username o correo. */
    @GetMapping
    public ResponseEntity<PageResponse<AdminUserResponse>> list(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {
        var users = adminUserService.listUsers(BearerTokens.require(authHeader), search, page, size);
        return ResponseEntity.ok(PageResponse.from(users, AdminUserResponse::from));
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<AdminUserResponse> changeRole(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @PathVariable UUID id,
            @Valid @RequestBody ChangeRoleRequest request) {
        var user = adminUserService.changeRole(BearerTokens.require(authHeader), id, request.role());
        return ResponseEntity.ok(AdminUserResponse.from(user));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<AdminUserResponse> changeStatus(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @PathVariable UUID id,
            @Valid @RequestBody ChangeStatusRequest request) {
        var user = adminUserService.changeStatus(BearerTokens.require(authHeader), id, request.isActive());
        return ResponseEntity.ok(AdminUserResponse.from(user));
    }
}
