package co.uceva.contests_platform.contest.application.usecase.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.uceva.contests_platform.contest.infrastructure.web.service.ContestSchedulerService;
import co.uceva.contests_platform.contest.application.usecase.UpdateContestUseCase;
import co.uceva.contests_platform.contest.domain.exception.ContestNotFoundException;
import co.uceva.contests_platform.contest.domain.model.Contest;
import co.uceva.contests_platform.contest.domain.repository.ContestRepository;
import co.uceva.contests_platform.contest.domain.service.PasswordEncoder;
import co.uceva.contests_platform.contest.domain.valueobject.ContestPeriod;

/**
 * Servicio de Aplicación que implementa el caso de uso de actualización de un concurso.
 */
@Service
public class UpdateContestUseCaseImpl implements UpdateContestUseCase {

    private final ContestRepository contestRepository;
    private final PasswordEncoder passwordEncoder;
    private final ContestSchedulerService scheduler;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param contestRepository Puerto de salida para gestionar concursos.
     * @param passwordEncoder   Servicio de dominio para cifrar contraseñas.
     */
    public UpdateContestUseCaseImpl(ContestRepository contestRepository, PasswordEncoder passwordEncoder,
                                    ContestSchedulerService scheduler) {
        this.scheduler = scheduler;
        this.contestRepository = contestRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Ejecuta la actualización de un concurso:
     * 1. Busca el concurso existente.
     * 2. Cifra la nueva contraseña, o conserva la actual si no se envía una.
     * 3. Construye el nuevo período; si la fecha de inicio no cambió se restaura
     *    sin exigir que sea futura, para poder editar un concurso ya iniciado.
     * 4. Actualiza la entidad de dominio y persiste los cambios.
     *
     * @param command Datos de entrada para actualizar el concurso.
     * @return El concurso actualizado y persistido.
     * @throws ContestNotFoundException Si el concurso no existe.
     */
    @Override
    @Transactional
    public Contest execute(UpdateContestCommand command) {
        Contest contest = contestRepository.findById(command.contestId())
                .orElseThrow(() -> new ContestNotFoundException(command.contestId()));

        String passwordHash = (command.password() == null || command.password().isBlank())
                ? contest.getPasswordHash()
                : passwordEncoder.encode(command.password());

        ContestPeriod period = command.startTime() != null && command.startTime().equals(contest.getStartTime())
                ? ContestPeriod.restore(command.startTime(), command.endTime())
                : ContestPeriod.of(command.startTime(), command.endTime());

        contest.update(
                command.title(),
                command.description(),
                passwordHash,
                period,
                command.status()
        );
        if (command.freezeMinutes() != null) {
            contest.changeFreezeMinutes(command.freezeMinutes());
        }

        Contest saved = contestRepository.save(contest);
        scheduler.scheduleContest(saved);
        return saved;
    }
}
