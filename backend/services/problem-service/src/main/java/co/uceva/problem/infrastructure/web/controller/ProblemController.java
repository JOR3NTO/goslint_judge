package co.uceva.problem.infrastructure.web.controller;

import co.uceva.problem.application.usecase.*;
import co.uceva.problem.domain.model.Problem;
import co.uceva.problem.infrastructure.web.dto.*;
import co.uceva.problem.infrastructure.web.mapper.ProblemWebMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controlador REST que maneja las solicitudes HTTP relacionadas con los Problemas.
 * Pertenece a la capa de Infraestructura y delega toda la lógica de negocio
 * a los puertos de entrada (casos de uso).
 */
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/v1/problems")
public class ProblemController {

    private final CreateProblemUseCase createProblemUseCase;
    private final GetProblemByIdUseCase getProblemByIdUseCase;
    private final GetAllProblemsByTitleUseCase getAllProblemsByTitleUseCase;
    private final GetAllProblemsUseCase getAllProblemsUseCase;
    private final UpdateProblemUseCase updateProblemUseCase;
    private final DeleteProblemUseCase deleteProblemUseCase;

    /**
     * Inyección de dependencias para obtener los orquestadores de cada caso de uso.
     *
     * @param createProblemUseCase        Caso de uso de creación.
     * @param getProblemByIdUseCase       Caso de uso de consulta por ID.
     * @param getAllProblemsByTitleUseCase Caso de uso de búsqueda por título.
     * @param getAllProblemsUseCase       Caso de uso de consulta general.
     * @param updateProblemUseCase        Caso de uso de actualización.
     * @param deleteProblemUseCase        Caso de uso de eliminación.
     */
    public ProblemController(
            CreateProblemUseCase createProblemUseCase,
            GetProblemByIdUseCase getProblemByIdUseCase,
            GetAllProblemsByTitleUseCase getAllProblemsByTitleUseCase,
            GetAllProblemsUseCase getAllProblemsUseCase,
            UpdateProblemUseCase updateProblemUseCase,
            DeleteProblemUseCase deleteProblemUseCase) {
        this.createProblemUseCase = createProblemUseCase;
        this.getProblemByIdUseCase = getProblemByIdUseCase;
        this.getAllProblemsByTitleUseCase = getAllProblemsByTitleUseCase;
        this.getAllProblemsUseCase = getAllProblemsUseCase;
        this.updateProblemUseCase = updateProblemUseCase;
        this.deleteProblemUseCase = deleteProblemUseCase;
    }

    /**
     * Endpoint para crear un nuevo problema.
     * <p>
     * Requiere rol {@code ADMIN} u {@code ORGANIZER}.
     * </p>
     *
     * @param request DTO con los datos del problema.
     * @return 201 CREATED con el problema creado.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZER')")
    public ResponseEntity<ProblemResponseDTO> create(@RequestBody CreateProblemRequestDTO request) {
        // Convertir el DTO a comando de aplicación
        var command = ProblemWebMapper.toCommand(request);
        // Delegar la creación al caso de uso
        Problem created = createProblemUseCase.execute(command);
        // Construir y retornar la respuesta HTTP
        return ResponseEntity.status(HttpStatus.CREATED).body(ProblemWebMapper.toResponse(created));
    }

    /**
     * Endpoint para obtener un problema por su identificador.
     * <p>
     * Acceso público.
     * </p>
     *
     * @param id Identificador del problema.
     * @return 200 OK con el problema encontrado.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProblemResponseDTO> getById(@PathVariable("id") UUID id) {
        Problem problem = getProblemByIdUseCase.execute(id);
        return ResponseEntity.ok(ProblemWebMapper.toResponse(problem));
    }

    /**
     * Endpoint para buscar problemas por título.
     * <p>
     * Acceso público.
     * </p>
     *
     * @param title Título o fragmento a buscar.
     * @return 200 OK con la lista de problemas coincidentes.
     */
    @GetMapping("/title/{title}")
    public ResponseEntity<List<ProblemResponseDTO>> getAllByTitle(@PathVariable("title") String title) {
        List<Problem> problems = getAllProblemsByTitleUseCase.execute(title);
        List<ProblemResponseDTO> problemsResponce = problems.stream()
                .map(ProblemWebMapper::toResponse)
                .toList();
        return ResponseEntity.ok(problemsResponce);
    }

    /**
     * Endpoint para obtener todos los problemas registrados.
     * <p>
     * Acceso público.
     * </p>
     *
     * @return 200 OK con la lista de problemas.
     */
    @GetMapping("/all")
    public ResponseEntity<List<ProblemResponseDTO>> getAll() {
        List<Problem> problems = getAllProblemsUseCase.execute();
        List<ProblemResponseDTO> problemsResponce = problems.stream()
                .map(ProblemWebMapper::toResponse)
                .toList();
        return ResponseEntity.ok(problemsResponce);
    }

    /**
     * Endpoint para actualizar un problema existente.
     * <p>
     * Requiere rol {@code ADMIN} u {@code ORGANIZER}.
     * </p>
     *
     * @param id      Identificador del problema a actualizar.
     * @param request DTO con los nuevos datos.
     * @return 200 OK con el problema actualizado.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZER')")
    public ResponseEntity<ProblemResponseDTO> update(
            @PathVariable("id") UUID id,
            @RequestBody UpdateProblemRequestDTO request) {
        // Convertir el DTO a comando de aplicación
        var command = ProblemWebMapper.toCommand(id, request);
        // Delegar la actualización al caso de uso
        Problem updated = updateProblemUseCase.execute(command);
        // Construir y retornar la respuesta HTTP
        return ResponseEntity.ok(ProblemWebMapper.toResponse(updated));
    }

    /**
     * Endpoint para eliminar un problema.
     * <p>
     * Requiere rol {@code ADMIN} u {@code ORGANIZER}.
     * </p>
     *
     * @param id Identificador del problema a eliminar.
     * @return 204 NO CONTENT.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZER')")
    public ResponseEntity<Void> delete(@PathVariable("id") UUID id) {
        deleteProblemUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }
}