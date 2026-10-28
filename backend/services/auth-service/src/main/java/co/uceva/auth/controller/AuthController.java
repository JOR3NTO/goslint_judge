package co.uceva.auth.controller;

import co.uceva.auth.dto.ChangePasswordRequest;
import co.uceva.auth.dto.LoginRequest;
import co.uceva.auth.dto.LoginResponse;
import co.uceva.auth.dto.RefreshRequest;
import co.uceva.auth.dto.RegisterRequest;
import co.uceva.auth.dto.UpdateUsernameRequest;
import co.uceva.auth.dto.UserProfileResponse;
import co.uceva.auth.exception.BadCredentialsException;
import co.uceva.auth.model.User;
import co.uceva.auth.service.AuthService;
import co.uceva.auth.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoints de autenticación y de perfil del usuario autenticado.
 * Solo traduce HTTP; la lógica vive en {@link AuthService} y {@link ProfileService}.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;
    private final ProfileService profileService;

    public AuthController(AuthService authService, ProfileService profileService) {
        this.authService = authService;
        this.profileService = profileService;
    }

    /** 201 con el id del usuario; 409 si el correo o el username ya existen. */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Usuario registrado exitosamente", "userId", user.getId()));
    }

    /** 200 con access y refresh token; 401 credenciales incorrectas; 423 cuenta bloqueada. */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request.email(), request.password()));
    }

    /**
     * Siempre responde 200: para el cliente el logout es incondicional, aunque el
     * token falte o ya no sea válido.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            authService.logout(authHeader.substring(BEARER_PREFIX.length()).strip());
        }
        return ResponseEntity.ok().build();
    }

    /** Canjea un refresh token por un par nuevo. 401 si el refresh token no sirve. */
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
    }

    /** Perfil del usuario dueño del token. */
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {
        User user = profileService.getCurrentUser(bearerToken(authHeader));
        return ResponseEntity.ok(UserProfileResponse.from(user));
    }

    /** 200 con el perfil actualizado; 409 si el username ya está en uso. */
    @PatchMapping("/me/username")
    public ResponseEntity<UserProfileResponse> updateUsername(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @Valid @RequestBody UpdateUsernameRequest request) {
        User user = profileService.updateUsername(bearerToken(authHeader), request.username());
        return ResponseEntity.ok(UserProfileResponse.from(user));
    }

    /**
     * 204 si se cambió; 422 si la contraseña actual es incorrecta. Cierra todas las
     * sesiones, así que el cliente debe iniciar sesión de nuevo.
     */
    @PatchMapping("/me/password")
    public ResponseEntity<Void> changePassword(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @Valid @RequestBody ChangePasswordRequest request) {
        profileService.changePassword(bearerToken(authHeader), request.currentPassword(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    private String bearerToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            throw new BadCredentialsException("Token no proporcionado o formato inválido");
        }
        return authHeader.substring(BEARER_PREFIX.length()).strip();
    }
}
