package co.uceva.auth.dto;

/**
 * Reglas de validación compartidas por los DTOs de entrada, para que registro
 * y cambio de contraseña/usuario exijan exactamente lo mismo.
 */
public final class ValidationRules {

    private ValidationRules() {
    }

    /** Mínimo 8 caracteres con al menos una mayúscula, un número y un carácter especial. */
    public static final String PASSWORD_PATTERN = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).{8,}$";
    public static final String PASSWORD_MESSAGE =
            "La contraseña debe tener al menos 8 caracteres, una mayúscula, un número y un carácter especial";

    /** Entre 3 y 30 caracteres: letras, dígitos, guion bajo y punto. */
    public static final String USERNAME_PATTERN = "^[a-zA-Z0-9_.]{3,30}$";
    public static final String USERNAME_MESSAGE =
            "El nombre de usuario debe tener entre 3 y 30 caracteres y solo puede contener letras, dígitos, guion bajo (_) y punto (.)";
}
