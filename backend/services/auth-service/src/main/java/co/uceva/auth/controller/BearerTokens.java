package co.uceva.auth.controller;

import co.uceva.auth.exception.BadCredentialsException;

/** Extrae el token del header {@code Authorization: Bearer <token>}. */
final class BearerTokens {

    private static final String PREFIX = "Bearer ";

    private BearerTokens() {
    }

    /** @throws BadCredentialsException si el header falta o no usa el esquema Bearer. */
    static String require(String authHeader) {
        String token = optional(authHeader);
        if (token == null) {
            throw new BadCredentialsException("Token no proporcionado o formato inválido");
        }
        return token;
    }

    /** @return el token, o {@code null} si el header falta o no usa el esquema Bearer. */
    static String optional(String authHeader) {
        if (authHeader == null || !authHeader.startsWith(PREFIX)) {
            return null;
        }
        return authHeader.substring(PREFIX.length()).strip();
    }
}
