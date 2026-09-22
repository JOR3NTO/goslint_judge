package co.uceva.problem.application.usecase.impl;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.uceva.problem.application.usecase.CreateProblemUseCase;
import co.uceva.problem.domain.model.Problem;
import co.uceva.problem.domain.repository.ProblemRepository;

/**
 * Servicio de Aplicación que implementa el caso de uso de creación de problemas.
 * Orquesta la construcción de la entidad de dominio y su persistencia.
 */
@Service
public class CreateProblemUseCaseImpl implements CreateProblemUseCase{

    private final ProblemRepository problemRepository;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param problemRepository Puerto de salida para persistir problemas.
     */
    public CreateProblemUseCaseImpl(ProblemRepository problemRepository) {
        this.problemRepository = problemRepository;
    }

    /**
     * Ejecuta el flujo de creación de un problema:
     * 1. Construye la entidad de dominio a partir del comando.
     * 2. Persiste el problema a través del repositorio.
     *
     * @param command Datos de entrada para crear el problema.
     * @return El problema creado y persistido.
     */
    @Override
    @Transactional
    public Problem execute(CreateProblemCommand command) {
        // Construir la entidad de dominio usando el factory method
        Problem problem = Problem.create(
            command.createdBy(),
            command.title(),
            command.statement(),
            command.timeLimitMs(),
            command.memoryLimitKb(),
            command.difficulty(),
            command.inputFormat(),
            command.outputFormat()
        );

        // Persistir el problema a través del puerto de salida
        return problemRepository.save(problem);
    }
}