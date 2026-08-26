package co.uceva.submission.infrastructure.config;

import co.uceva.shared.infrastructure.security.JwtTokenValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declara el validador de los tokens emitidos por {@code auth-service}.
 * <p>
 * Vive aparte de {@link SecurityConfig} porque son dos cosas distintas: aquella
 * configura la cadena de filtros HTTP, y esto provee la pieza que reconoce a un
 * usuario a partir de su token. Hoy su único consumidor es el handshake del
 * WebSocket; cuando llegue el filtro JWT de los endpoints HTTP, usará este mismo
 * bean y ambos lados validarán exactamente igual.
 * </p>
 */
@Configuration
public class JwtConfig {

    /**
     * Validador de los tokens emitidos por {@code auth-service}.
     * <p>
     * La clase vive en {@code common-infrastructure} y no lleva anotaciones de
     * Spring, porque los paquetes de esa librería quedan fuera del escaneo de
     * componentes del servicio. Se declara aquí para que la clave y el emisor
     * salgan de la configuración de este servicio.
     * </p>
     * <p>
     * La clave no tiene valor por defecto a propósito: un secreto de ejemplo
     * heredado sin querer en producción aceptaría tokens falsificados por
     * cualquiera que conociese el repositorio. Sin la variable de entorno, el
     * servicio no arranca.
     * </p>
     *
     * @param secret Clave compartida con la que {@code auth-service} firma los tokens.
     * @param issuer Emisor esperado en los tokens.
     * @return Validador listo para verificar tokens.
     */
    @Bean
    public JwtTokenValidator jwtTokenValidator(
            @Value("${app.security.jwt.secret}") String secret,
            @Value("${app.security.jwt.issuer}") String issuer) {
        return new JwtTokenValidator(secret, issuer);
    }
}
