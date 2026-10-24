package co.uceva.contest.application.usecase.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.uceva.contest.application.usecase.CreateContestUseCase;
import co.uceva.contest.domain.model.Contest;
import co.uceva.contest.domain.repository.ContestRepository;
import co.uceva.contest.domain.service.PasswordEncoder;

/**
 * Servicio de Aplicación que implementa el caso de uso de creación de concursos.
 * Orquesta el cifrado de la contraseña, la construcción de la entidad y su persistencia.
 */
@Service
public class CreateContestUseCaseImpl implements CreateContestUseCase {

    private final ContestRepository contestRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param contestRepository Puerto de salida para persistir concursos.
     * @param passwordEncoder   Servicio de dominio para cifrar contraseñas.
     */
    public CreateContestUseCaseImpl(ContestRepository contestRepository, PasswordEncoder passwordEncoder) {
        this.contestRepository = contestRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Ejecuta el flujo de creación de un concurso:
     * 1. Cifra la contraseña de inscripción.
     * 2. Construye la entidad de dominio (que valida el período) a partir del comando.
     * 3. Persiste el concurso a través del repositorio.
     *
     * @param command Datos de entrada para crear el concurso.
     * @return El concurso creado y persistido.
     */
    @Override
    @Transactional
    public Contest execute(CreateContestCommand command) {
        String passwordHash = passwordEncoder.encode(command.password());

        Contest contest = Contest.create(
            command.createdBy(),
            command.title(),
            command.description(),
            passwordHash,
            command.startTime(),
            command.endTime()
        );

        return contestRepository.save(contest);
    }
}
