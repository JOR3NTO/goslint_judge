package co.uceva.contest.application.usecase.impl;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.uceva.contest.application.usecase.GetContestByIdUseCase;
import co.uceva.contest.domain.exception.ContestNotFoundException;
import co.uceva.contest.domain.model.Contest;
import co.uceva.contest.domain.repository.ContestRepository;

/**
 * Servicio de Aplicación que implementa la consulta de un concurso por identificador.
 */
@Service
public class GetContestByIdUseCaseImpl implements GetContestByIdUseCase {

    private final ContestRepository contestRepository;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param contestRepository Puerto de salida para consultar concursos.
     */
    public GetContestByIdUseCaseImpl(ContestRepository contestRepository) {
        this.contestRepository = contestRepository;
    }

    /**
     * Busca un concurso por su identificador y lanza una excepción si no existe.
     *
     * @param contestId Identificador del concurso.
     * @return El concurso encontrado.
     * @throws ContestNotFoundException Si el concurso no existe.
     */
    @Override
    @Transactional(readOnly = true)
    public Contest execute(UUID contestId) {
        return contestRepository.findById(contestId)
                .orElseThrow(() -> new ContestNotFoundException(contestId));
    }
}
