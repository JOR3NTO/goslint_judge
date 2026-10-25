package co.uceva.contest.application.usecase.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.uceva.contest.application.usecase.AskClarificationUseCase;
import co.uceva.contest.domain.exception.ContestProblemNotFoundException;
import co.uceva.contest.domain.exception.InvalidClarificationException;
import co.uceva.contest.domain.exception.TeamNotFoundException;
import co.uceva.contest.domain.model.Clarification;
import co.uceva.contest.domain.model.ContestProblem;
import co.uceva.contest.domain.model.Team;
import co.uceva.contest.domain.repository.ClarificationRepository;
import co.uceva.contest.domain.repository.ContestProblemRepository;
import co.uceva.contest.domain.repository.TeamRepository;

/**
 * Servicio de Aplicación que implementa el caso de uso de envío de preguntas de un equipo.
 */
@Service
public class AskClarificationUseCaseImpl implements AskClarificationUseCase {

    private final ClarificationRepository clarificationRepository;
    private final ContestProblemRepository contestProblemRepository;
    private final TeamRepository teamRepository;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param clarificationRepository  Puerto de salida para persistir aclaraciones.
     * @param contestProblemRepository Puerto de salida para consultar los problemas del concurso.
     * @param teamRepository           Puerto de salida para consultar equipos.
     */
    public AskClarificationUseCaseImpl(ClarificationRepository clarificationRepository,
                                       ContestProblemRepository contestProblemRepository,
                                       TeamRepository teamRepository) {
        this.clarificationRepository = clarificationRepository;
        this.contestProblemRepository = contestProblemRepository;
        this.teamRepository = teamRepository;
    }

    /**
     * Ejecuta el flujo de envío de una pregunta:
     * 1. Verifica que el problema del concurso exista.
     * 2. Busca el equipo del usuario en el concurso de ese problema.
     * 3. Crea la aclaración pendiente a nombre del equipo y la persiste.
     *
     * @param command Datos de entrada para enviar la pregunta.
     * @return La aclaración creada y persistida.
     * @throws ContestProblemNotFoundException Si el problema no está en ningún concurso.
     * @throws TeamNotFoundException           Si el usuario no pertenece a un equipo de ese concurso.
     * @throws InvalidClarificationException   Si la pregunta es inválida.
     */
    @Override
    @Transactional
    public Clarification execute(AskClarificationCommand command) {
        ContestProblem problem = contestProblemRepository.findById(command.problemId())
                .orElseThrow(() -> new ContestProblemNotFoundException(command.problemId()));

        Team team = teamRepository.findByContestIdAndUserId(problem.getContestId(), command.userId())
                .orElseThrow(() -> new TeamNotFoundException("El usuario '" + command.userId()
                        + "' no pertenece a ningún equipo del concurso '" + problem.getContestId() + "'."));

        return clarificationRepository.save(Clarification.ask(problem.getId(), team.getId(), command.question()));
    }
}
