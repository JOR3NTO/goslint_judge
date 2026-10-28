package co.uceva.contest.infrastructure.web.controller;

import java.util.List;
import java.util.UUID;

import co.uceva.contest.application.usecase.*;
import co.uceva.contest.domain.model.Contest;
import co.uceva.contest.infrastructure.web.dto.*;
import co.uceva.contest.infrastructure.web.mapper.ContestWebMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST que maneja las solicitudes HTTP relacionadas con los Concursos.
 * Pertenece a la capa de Infraestructura y delega toda la lógica de negocio
 * a los puertos de entrada (casos de uso).
 */
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/v1/contests")
public class ContestController {

    private final CreateContestUseCase createContestUseCase;
    private final GetContestByIdUseCase getContestByIdUseCase;
    private final GetAllContestsByTitleUseCase getAllContestsByTitleUseCase;
    private final GetAllContestsByCreatorUseCase getAllContestsByCreatorUseCase;
    private final GetAllContestsUseCase getAllContestsUseCase;
    private final UpdateContestUseCase updateContestUseCase;
    private final DeleteContestUseCase deleteContestUseCase;

    /**
     * Inyección de dependencias para obtener los orquestadores de cada caso de uso.
     *
     * @param createContestUseCase           Caso de uso de creación.
     * @param getContestByIdUseCase          Caso de uso de consulta por ID.
     * @param getAllContestsByTitleUseCase   Caso de uso de búsqueda por título.
     * @param getAllContestsByCreatorUseCase Caso de uso de consulta por creador.
     * @param getAllContestsUseCase          Caso de uso de consulta general.
     * @param updateContestUseCase           Caso de uso de actualización.
     * @param deleteContestUseCase           Caso de uso de eliminación.
     */
    public ContestController(
            CreateContestUseCase createContestUseCase,
            GetContestByIdUseCase getContestByIdUseCase,
            GetAllContestsByTitleUseCase getAllContestsByTitleUseCase,
            GetAllContestsByCreatorUseCase getAllContestsByCreatorUseCase,
            GetAllContestsUseCase getAllContestsUseCase,
            UpdateContestUseCase updateContestUseCase,
            DeleteContestUseCase deleteContestUseCase) {
        this.createContestUseCase = createContestUseCase;
        this.getContestByIdUseCase = getContestByIdUseCase;
        this.getAllContestsByTitleUseCase = getAllContestsByTitleUseCase;
        this.getAllContestsByCreatorUseCase = getAllContestsByCreatorUseCase;
        this.getAllContestsUseCase = getAllContestsUseCase;
        this.updateContestUseCase = updateContestUseCase;
        this.deleteContestUseCase = deleteContestUseCase;
    }

    /**
     * Endpoint para crear un nuevo concurso.
     * <p>
     * Requiere rol {@code ADMIN} u {@code ORGANIZER}.
     * </p>
     *
     * @param request DTO con los datos del concurso.
     * @return 201 CREATED con el concurso creado.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZER')")
    public ResponseEntity<ContestResponseDTO> create(@RequestBody CreateContestRequestDTO request) {
        var command = ContestWebMapper.toCommand(request);
        Contest created = createContestUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(ContestWebMapper.toResponse(created));
    }

    /**
     * Endpoint para obtener un concurso por su identificador.
     * <p>
     * Acceso público.
     * </p>
     *
     * @param id Identificador del concurso.
     * @return 200 OK con el concurso encontrado.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ContestResponseDTO> getById(@PathVariable("id") UUID id) {
        Contest contest = getContestByIdUseCase.execute(id);
        return ResponseEntity.ok(ContestWebMapper.toResponse(contest));
    }

    /**
     * Endpoint para buscar concursos por título.
     * <p>
     * Acceso público.
     * </p>
     *
     * @param title Título o fragmento a buscar.
     * @return 200 OK con la lista de concursos coincidentes.
     */
    @GetMapping("/title/{title}")
    public ResponseEntity<List<ContestResponseDTO>> getAllByTitle(@PathVariable("title") String title) {
        List<ContestResponseDTO> contests = getAllContestsByTitleUseCase.execute(title).stream()
                .map(ContestWebMapper::toResponse)
                .toList();
        return ResponseEntity.ok(contests);
    }

    /**
     * Endpoint para obtener los concursos creados por un usuario.
     * <p>
     * Acceso público.
     * </p>
     *
     * @param createdBy Identificador del usuario creador.
     * @return 200 OK con la lista de concursos del creador.
     */
    @GetMapping("/creator/{createdBy}")
    public ResponseEntity<List<ContestResponseDTO>> getAllByCreator(@PathVariable("createdBy") UUID createdBy) {
        List<ContestResponseDTO> contests = getAllContestsByCreatorUseCase.execute(createdBy).stream()
                .map(ContestWebMapper::toResponse)
                .toList();
        return ResponseEntity.ok(contests);
    }

    /**
     * Endpoint para obtener todos los concursos registrados.
     * <p>
     * Acceso público.
     * </p>
     *
     * @return 200 OK con la lista de concursos.
     */
    @GetMapping()
    public ResponseEntity<List<ContestResponseDTO>> getAll() {
        List<ContestResponseDTO> contests = getAllContestsUseCase.execute().stream()
                .map(ContestWebMapper::toResponse)
                .toList();
        return ResponseEntity.ok(contests);
    }

    /**
     * Endpoint para actualizar un concurso existente.
     * <p>
     * Requiere rol {@code ADMIN} u {@code ORGANIZER}.
     * </p>
     *
     * @param id      Identificador del concurso a actualizar.
     * @param request DTO con los nuevos datos.
     * @return 200 OK con el concurso actualizado.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZER')")
    public ResponseEntity<ContestResponseDTO> update(
            @PathVariable("id") UUID id,
            @RequestBody UpdateContestRequestDTO request) {
        var command = ContestWebMapper.toCommand(id, request);
        Contest updated = updateContestUseCase.execute(command);
        return ResponseEntity.ok(ContestWebMapper.toResponse(updated));
    }

    /**
     * Endpoint para eliminar un concurso.
     * <p>
     * Requiere rol {@code ADMIN} u {@code ORGANIZER}.
     * </p>
     *
     * @param id Identificador del concurso a eliminar.
     * @return 204 NO CONTENT.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZER')")
    public ResponseEntity<Void> delete(@PathVariable("id") UUID id) {
        deleteContestUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }
}
