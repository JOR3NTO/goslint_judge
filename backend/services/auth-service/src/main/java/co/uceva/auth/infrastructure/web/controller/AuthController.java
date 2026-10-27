package co.uceva.auth.infrastructure.web.controller;

import co.uceva.auth.application.usecase.GetCurrentUserUseCase;
import co.uceva.auth.application.usecase.LoginUserUseCase;
import co.uceva.auth.application.usecase.LogoutUserUseCase;
import co.uceva.auth.application.usecase.RegisterUserUseCase;
import co.uceva.auth.domain.exception.BadCredentialsException;
import co.uceva.auth.domain.model.AuthToken;
import co.uceva.auth.domain.model.User;
import co.uceva.auth.infrastructure.web.dto.LoginRequest;
import co.uceva.auth.infrastructure.web.dto.LoginResponse;
import co.uceva.auth.infrastructure.web.dto.RegisterRequest;
import co.uceva.auth.infrastructure.web.dto.UserProfileResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controlador REST que maneja las solicitudes HTTP relacionadas con la Autenticación.
 * Pertenece a la capa de Infraestructura y delega toda la lógica de negocio
 * al puerto de entrada (RegisterUserUseCase, LoginUserUseCase).
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final LoginUserUseCase loginUserUseCase;
    private final LogoutUserUseCase logoutUserUseCase;
    private final GetCurrentUserUseCase getCurrentUserUseCase;

    /**
     * Inyección de dependencias para obtener el orquestador del caso de uso.
     */
    public AuthController(RegisterUserUseCase registerUserUseCase,
                          LoginUserUseCase loginUserUseCase,
                          LogoutUserUseCase logoutUserUseCase,
                          GetCurrentUserUseCase getCurrentUserUseCase) {
        this.registerUserUseCase = registerUserUseCase;
        this.loginUserUseCase = loginUserUseCase;
        this.logoutUserUseCase = logoutUserUseCase;
        this.getCurrentUserUseCase = getCurrentUserUseCase;
    }

    /**
     * Cierra la sesión del usuario invalidando el access token activo.
     * Siempre devuelve 200 OK: el logout es incondicional desde la perspectiva
     * del cliente, independientemente del estado del token en el servidor.
     *
     * @param authHeader Header Authorization: Bearer &lt;token&gt; (puede ser null).
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            logoutUserUseCase.logout(authHeader.substring(7).strip());
        }
        return ResponseEntity.ok().build();
    }

    /**
     * Endpoint para iniciar sesión en la plataforma.
     * @param request JSON con email y contraseña.
     * @return 200 OK si tiene éxito, con el access token y refresh token.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthToken authToken = loginUserUseCase.login(request.getEmail(), request.getPassword());
        
        LoginResponse response = LoginResponse.builder()
                .accessToken(authToken.getAccessToken())
                .refreshToken(authToken.getRefreshToken())
                .expiresIn(authToken.getExpiresIn())
                .type("Bearer")
                .build();
                
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint para registrar un nuevo usuario en la plataforma.
     * La anotación @Valid activa las validaciones definidas en el DTO.
     * 
     * @param request JSON con los datos del usuario.
     * @return 201 CREATED si tiene éxito, con el ID del nuevo usuario.
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        // Delegar al caso de uso de negocio
        User registeredUser = registerUserUseCase.register(
                request.getFirstName(),
                request.getLastName(),
                request.getUsername(),
                request.getEmail(),
                request.getPassword(),
                request.getInstitution()
        );

        // Construir la respuesta HTTP
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Usuario registrado exitosamente");
        response.put("userId", registeredUser.getId());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BadCredentialsException("Token no proporcionado o formato inválido");
        }
        String rawToken = authHeader.substring(7).strip();
        User user = getCurrentUserUseCase.getCurrentUser(rawToken);
        return ResponseEntity.ok(UserProfileResponse.fromDomain(user));
    }
}
