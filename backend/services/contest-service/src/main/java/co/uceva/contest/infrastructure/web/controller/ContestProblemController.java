package co.uceva.contest.infrastructure.web.controller;

import java.util.List;
import java.util.UUID;

import co.uceva.contest.application.usecase.*;
import co.uceva.contest.infrastructure.web.dto.*;
import co.uceva.contest.infrastructure.web.mapper.ContestProblemWebMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST de los problemas de un concurso. Delega toda la lógica en los casos de uso.
 * Asignar, editar y retirar problemas exige rol {@code ADMIN} u {@code ORGANIZER}; la consulta es pública.
 */
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/v1")
public class ContestProblemController {

    private final AddProblemToContestUseCase addProblemToContestUseCase;
    private final UpdateContestProblemUseCase updateContestProblemUseCase;
    private final RemoveProblemFromContestUseCase removeProblemFromContestUseCase;
    private final GetProblemsByContestUseCase getProblemsByContestUseCase;

    /**
     * Inyección de dependencias para obtener los orquestadores de cada caso de uso.
     *
     * @param addProblemToContestUseCase      Caso de uso de asignación de un problema a un concurso.
     * @param updateContestProblemUseCase     Caso de uso de actualización de un problema del concurso.
     * @param removeProblemFromContestUseCase Caso de uso de retiro de un problema del concurso.
     * @param getProblemsByContestUseCase     Caso de uso de consulta de los problemas de un concurso.
     */
    public ContestProblemController(
            AddProblemToContestUseCase addProblemToContestUseCase,
            UpdateContestProblemUseCase updateContestProblemUseCase,
            RemoveProblemFromContestUseCase removeProblemFromContestUseCase,
            GetProblemsByContestUseCase getProblemsByContestUseCase) {
        this.addProblemToContestUseCase = addProblemToContestUseCase;
        this.updateContestProblemUseCase = updateContestProblemUseCase;
        this.removeProblemFromContestUseCase = removeProblemFromContestUseCase;
        this.getProblemsByContestUseCase = getProblemsByContestUseCase;
    }

    /**
     * Asigna un problema existente en problem-service al concurso.
     * <p>
     * Solo {@code ADMIN} u {@code ORGANIZER}. Se verifica que el concurso y el problema existan, y que
     * el problema y el alias no estén ya en uso en ese concurso.
     * </p>
     *
     * @param contestId Identificador del concurso.
     * @param request   Problema a asignar con su alias, posición, puntos (1 si se omiten) y color del globo.
     * @return 201 CREATED con la asignación creada; 400 si algún valor es inválido; 404 si el concurso o el
     *         problema no existen; 409 si el problema o el alias ya están en el concurso; 503 si no se pudo
     *         consultar problem-service.
     */
    @PostMapping("/contests/{contestId}/problems")
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZER')")
    public ResponseEntity<ContestProblemResponseDTO> add(
            @PathVariable("contestId") UUID contestId,
            @RequestBody AddContestProblemRequestDTO request) {
        var created = addProblemToContestUseCase.execute(ContestProblemWebMapper.toCommand(contestId, request));
        return ResponseEntity.status(HttpStatus.CREATED).body(ContestProblemWebMapper.toResponse(created));
    }

    /**
     * Lista los problemas del concurso ordenados por su posición de presentación.
     * <p>
     * Acceso público.
     * </p>
     *
     * @param contestId Identificador del concurso.
     * @return 200 OK con la lista de problemas (vacía si no tiene); 404 si el concurso no existe.
     */
    @GetMapping("/contests/{contestId}/problems")
    public ResponseEntity<List<ContestProblemResponseDTO>> getByContest(@PathVariable("contestId") UUID contestId) {
        return ResponseEntity.ok(getProblemsByContestUseCase.execute(contestId).stream()
                .map(ContestProblemWebMapper::toResponse).toList());
    }

    /**
     * Actualiza el alias, la posición, los puntos y el color del globo de un problema del concurso.
     * <p>
     * Solo {@code ADMIN} u {@code ORGANIZER}. No cambia el problema asignado ni el concurso.
     * </p>
     *
     * @param id      Identificador de la asignación (no el del problema en problem-service).
     * @param request Nuevos valores; todos son obligatorios.
     * @return 200 OK con la asignación actualizada; 400 si algún valor es inválido; 404 si la asignación
     *         no existe; 409 si el nuevo alias ya lo usa otro problema del concurso.
     */
    @PutMapping("/contest-problems/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZER')")
    public ResponseEntity<ContestProblemResponseDTO> update(
            @PathVariable("id") UUID id,
            @RequestBody UpdateContestProblemRequestDTO request) {
        var updated = updateContestProblemUseCase.execute(ContestProblemWebMapper.toCommand(id, request));
        return ResponseEntity.ok(ContestProblemWebMapper.toResponse(updated));
    }

    /**
     * Retira un problema del concurso. El problema sigue existiendo en problem-service.
     * <p>
     * Solo {@code ADMIN} u {@code ORGANIZER}.
     * </p>
     *
     * @param id Identificador de la asignación (no el del problema en problem-service).
     * @return 204 No Content si se retiró; 404 si la asignación no existe.
     */
    @DeleteMapping("/contest-problems/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZER')")
    public ResponseEntity<Void> remove(@PathVariable("id") UUID id) {
        removeProblemFromContestUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }
}
