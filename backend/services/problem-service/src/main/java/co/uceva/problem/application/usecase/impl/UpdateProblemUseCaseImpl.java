package co.uceva.problem.application.usecase.impl;

import co.uceva.problem.application.usecase.UpdateProblemUseCase;
import co.uceva.problem.domain.exception.ProblemNotFoundException;
import co.uceva.problem.domain.model.Problem;
import co.uceva.problem.domain.repository.ProblemRepository;
import co.uceva.problem.domain.valueobject.Difficulty;
import co.uceva.problem.domain.valueobject.MemoryLimit;
import co.uceva.problem.domain.valueobject.TimeLimit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de Aplicación que implementa el caso de uso de actualización de un problema.
 */
@Service
public class UpdateProblemUseCaseImpl implements UpdateProblemUseCase {

    private final ProblemRepository problemRepository;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param problemRepository Puerto de salida para gestionar problemas.
     */
    public UpdateProblemUseCaseImpl(ProblemRepository problemRepository) {
        this.problemRepository = problemRepository;
    }

    /**
     * Ejecuta la actualización de un problema:
     * 1. Busca el problema existente.
     * 2. Construye los value objects con las nuevas restricciones.
     * 3. Actualiza la entidad de dominio.
     * 4. Persiste los cambios.
     *
     * @param command Datos de entrada para actualizar el problema.
     * @return El problema actualizado y persistido.
     * @throws ProblemNotFoundException Si el problema no existe.
     */
    @Override
    @Transactional
    public Problem execute(UpdateProblemCommand command) {
        // Buscar el problema o lanzar excepción de dominio
        Problem problem = problemRepository.findById(command.problemId())
                .orElseThrow(() -> new ProblemNotFoundException(command.problemId()));

        // Construir value objects que validan las nuevas restricciones
        TimeLimit newTimeLimit = new TimeLimit(command.timeLimitMs());
        MemoryLimit newMemoryLimit = new MemoryLimit(command.memoryLimitKb());
        Difficulty newDifficulty = new Difficulty(command.difficultyRating());

        // Actualizar la entidad de dominio
        problem.update(
                command.title(),
                command.statement(),
                newTimeLimit,
                newMemoryLimit,
                newDifficulty,
                command.inputFormat(),
                command.outputFormat()
        );

        // Persistir los cambios
        return problemRepository.save(problem);
    }
}