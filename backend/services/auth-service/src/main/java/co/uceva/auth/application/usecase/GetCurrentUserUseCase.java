package co.uceva.auth.application.usecase;

import co.uceva.auth.domain.model.User;

/**
 * Puerto de entrada para obtener el perfil del usuario autenticado actual.
 */
public interface GetCurrentUserUseCase {
    /**
     * Obtiene la entidad del usuario autenticado validando su JWT.
     *
     * @param rawToken Token JWT compacto sin prefijo "Bearer ".
     * @return Entidad de dominio User con la información completa.
     * @throws co.uceva.auth.domain.exception.BadCredentialsException Si el token es inválido, expiró o está revocado.
     */
    User getCurrentUser(String rawToken);
}
