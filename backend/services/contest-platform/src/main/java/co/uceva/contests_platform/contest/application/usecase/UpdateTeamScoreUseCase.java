package co.uceva.contests_platform.contest.application.usecase;

import java.util.UUID;

/**
 * Puerto de entrada para el caso de uso: recalcular el puntaje y la penalización de un equipo
 * desde sus envíos.
 */
public interface UpdateTeamScoreUseCase {
    /**
     * Recalcular el puntaje y la penalización de un equipo desde sus envíos.
     *
     * @param teamId Identificador del equipo.
     */
    void execute(UUID teamId);
}
