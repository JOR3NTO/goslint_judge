package co.uceva.auth.application.usecase;

import co.uceva.auth.domain.model.AuthToken;

public interface LoginUserUseCase {
    AuthToken login(String email, String password);
}
