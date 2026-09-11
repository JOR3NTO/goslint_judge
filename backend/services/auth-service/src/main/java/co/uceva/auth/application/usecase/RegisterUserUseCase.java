package co.uceva.auth.application.port.in;

import co.uceva.auth.domain.model.User;

/**
 * Puerto de Entrada (Input Port) que define el caso de uso para registrar usuarios.
 * Cualquier componente externo (como un controlador web) debe comunicarse
 * a través de esta interfaz para invocar la lógica de negocio.
 */
public interface RegisterUserUseCase {
    /**
     * Orquesta el proceso de validación, encriptación y persistencia
     * para registrar a un nuevo usuario en la plataforma.
     *
     * @param username    El nombre de usuario deseado.
     * @param email       El correo electrónico.
     * @param password    La contraseña en texto plano (para ser cifrada aquí).
     * @param institution La institución (opcional).
     * @return El {@link User} creado y persistido con su ID asignado.
     * @throws co.uceva.auth.domain.exception.UserAlreadyExistsException si el username o email ya existen.
     */
    User register(String username, String email, String password, String institution);
}
