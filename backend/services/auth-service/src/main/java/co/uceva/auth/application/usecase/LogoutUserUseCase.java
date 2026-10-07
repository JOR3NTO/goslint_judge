package co.uceva.auth.application.usecase;

/**
 * Puerto de entrada para el cierre de sesión (HU-03).
 */
public interface LogoutUserUseCase {

    /**
     * Invalida el token dado insertándolo en la lista negra.
     * Si el token ya expiró o {@code rawToken} es nulo/vacío, no hace nada.
     *
     * @param rawToken Token JWT compacto SIN el prefijo "Bearer ".
     */
    void logout(String rawToken);
}
