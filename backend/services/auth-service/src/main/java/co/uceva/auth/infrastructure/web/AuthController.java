package co.uceva.auth.infrastructure.web;

import co.uceva.auth.application.port.in.LoginUserUseCase;
import co.uceva.auth.application.port.in.RegisterUserUseCase;
import co.uceva.auth.domain.model.AuthToken;
import co.uceva.auth.domain.model.User;
import co.uceva.auth.infrastructure.web.dto.LoginRequest;
import co.uceva.auth.infrastructure.web.dto.LoginResponse;
import co.uceva.auth.infrastructure.web.dto.RegisterRequest;
import jakarta.validation.Valid;
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

    /**
     * Inyección de dependencias para obtener el orquestador del caso de uso.
     */
    public AuthController(RegisterUserUseCase registerUserUseCase, LoginUserUseCase loginUserUseCase) {
        this.registerUserUseCase = registerUserUseCase;
        this.loginUserUseCase = loginUserUseCase;
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
}
