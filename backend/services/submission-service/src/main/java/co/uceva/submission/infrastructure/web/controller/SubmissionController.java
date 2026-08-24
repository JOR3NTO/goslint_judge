package co.uceva.submission.infrastructure.web.controller;

import co.uceva.submission.application.usecase.DeleteSubmissionUseCase;
import co.uceva.submission.application.usecase.GetAllSubmissionsUseCase;
import co.uceva.submission.application.usecase.GetSubmissionByIdUseCase;
import co.uceva.submission.application.usecase.GetSubmissionHistoryUseCase;
import co.uceva.submission.application.usecase.GetSubmissionMetricsUseCase;
import co.uceva.submission.application.usecase.GetSubmissionMetricsUseCase.SubmissionMetrics;
import co.uceva.submission.application.usecase.GetSubmissionsByProblemUseCase;
import co.uceva.submission.application.usecase.GetSubmissionsByTeamUseCase;
import co.uceva.submission.application.usecase.SubmitCodeUseCase;
import co.uceva.submission.application.usecase.GetSubmissionHistoryUseCase.GetSubmissionHistoryCommand;
import co.uceva.submission.domain.model.Submission;
import co.uceva.submission.infrastructure.web.dto.SubmitCodeRequestDTO;
import co.uceva.submission.infrastructure.web.dto.SubmissionMetricsResponseDTO;
import co.uceva.submission.infrastructure.web.dto.SubmissionResponseDTO;
import co.uceva.submission.infrastructure.web.mapper.SubmissionWebMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controlador REST que maneja las solicitudes HTTP relacionadas con los envíos de código fuente.
 * Pertenece a la capa de Infraestructura y delega toda la lógica de negocio
 * a los puertos de entrada (casos de uso).
 */
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/v1/submissions")
public class SubmissionController {

    private final SubmitCodeUseCase submitCodeUseCase;
    private final GetSubmissionByIdUseCase getSubmissionByIdUseCase;
    private final GetAllSubmissionsUseCase getAllSubmissionsUseCase;
    private final GetSubmissionsByProblemUseCase getSubmissionsByProblemUseCase;
    private final GetSubmissionsByTeamUseCase getSubmissionsByTeamUseCase;
    private final GetSubmissionHistoryUseCase getSubmissionHistoryUseCase;
    private final GetSubmissionMetricsUseCase getSubmissionMetricsUseCase;
    private final DeleteSubmissionUseCase deleteSubmissionUseCase;

    /**
     * Inyección de dependencias para obtener los orquestadores de cada caso de uso.
     */
    public SubmissionController(
            SubmitCodeUseCase submitCodeUseCase,
            GetSubmissionByIdUseCase getSubmissionByIdUseCase,
            GetAllSubmissionsUseCase getAllSubmissionsUseCase,
            GetSubmissionsByProblemUseCase getSubmissionsByProblemUseCase,
            GetSubmissionsByTeamUseCase getSubmissionsByTeamUseCase,
            GetSubmissionHistoryUseCase getSubmissionHistoryUseCase,
            GetSubmissionMetricsUseCase getSubmissionMetricsUseCase,
            DeleteSubmissionUseCase deleteSubmissionUseCase) {
        this.submitCodeUseCase = submitCodeUseCase;
        this.getSubmissionByIdUseCase = getSubmissionByIdUseCase;
        this.getAllSubmissionsUseCase = getAllSubmissionsUseCase;
        this.getSubmissionsByProblemUseCase = getSubmissionsByProblemUseCase;
        this.getSubmissionsByTeamUseCase = getSubmissionsByTeamUseCase;
        this.getSubmissionHistoryUseCase = getSubmissionHistoryUseCase;
        this.getSubmissionMetricsUseCase = getSubmissionMetricsUseCase;
        this.deleteSubmissionUseCase = deleteSubmissionUseCase;
    }

    /**
     * Endpoint para crear un nuevo envío de código fuente.
     * <p>
     * Requiere rol {@code STUDENT}, {@code ADMIN} u {@code ORGANIZER}.
     * </p>
     *
     * @param request DTO con los datos del envío.
     * @return 201 CREATED con el envío creado.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN','ORGANIZER')")
    public ResponseEntity<SubmissionResponseDTO> create(@RequestBody SubmitCodeRequestDTO request) {
        var command = SubmissionWebMapper.toCommand(request);
        Submission created = submitCodeUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(SubmissionWebMapper.toResponse(created));
    }

    /**
     * Endpoint para obtener un envío por su identificador.
     * <p>
     * Requiere rol {@code STUDENT}, {@code ADMIN}, {@code ORGANIZER} o {@code SERVICE}.
     * </p>
     *
     * @param id Identificador del envío.
     * @return 200 OK con el envío encontrado.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN','ORGANIZER','SERVICE')")
    public ResponseEntity<SubmissionResponseDTO> getById(@PathVariable("id") UUID id) {
        Submission submission = getSubmissionByIdUseCase.execute(id);
        return ResponseEntity.ok(SubmissionWebMapper.toResponse(submission));
    }

    /**
     * Endpoint para obtener todos los envíos registrados.
     * <p>
     * Requiere rol {@code ADMIN} u {@code ORGANIZER}.
     * </p>
     *
     * @return 200 OK con la lista de envíos.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZER')")
    public ResponseEntity<List<SubmissionResponseDTO>> getAll() {
        List<Submission> submissions = getAllSubmissionsUseCase.execute();
        List<SubmissionResponseDTO> response = submissions.stream()
                .map(SubmissionWebMapper::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint para obtener todos los envíos de un problema específico.
     * <p>
     * Requiere rol {@code ADMIN}, {@code ORGANIZER} o {@code SERVICE}.
     * </p>
     *
     * @param problemId Identificador del problema.
     * @return 200 OK con la lista de envíos.
     */
    @GetMapping("/problem/{problemId}")
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZER','SERVICE')")
    public ResponseEntity<List<SubmissionResponseDTO>> getByProblem(@PathVariable("problemId") UUID problemId) {
        List<Submission> submissions = getSubmissionsByProblemUseCase.execute(problemId);
        List<SubmissionResponseDTO> response = submissions.stream()
                .map(SubmissionWebMapper::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint para obtener todos los envíos de un equipo específico.
     * <p>
     * Requiere rol {@code STUDENT}, {@code ADMIN} u {@code ORGANIZER}.
     * </p>
     *
     * @param teamId Identificador del equipo.
     * @return 200 OK con la lista de envíos.
     */
    @GetMapping("/team/{teamId}")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN','ORGANIZER')")
    public ResponseEntity<List<SubmissionResponseDTO>> getByTeam(@PathVariable("teamId") UUID teamId) {
        List<Submission> submissions = getSubmissionsByTeamUseCase.execute(teamId);
        List<SubmissionResponseDTO> response = submissions.stream()
                .map(SubmissionWebMapper::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint para obtener el historial de envíos de un equipo en un problema.
     * <p>
     * Requiere rol {@code STUDENT}, {@code ADMIN} u {@code ORGANIZER}.
     * </p>
     *
     * @param problemId Identificador del problema.
     * @param teamId    Identificador del equipo.
     * @return 200 OK con la lista de envíos.
     */
    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN','ORGANIZER')")
    public ResponseEntity<List<SubmissionResponseDTO>> getHistory(
            @RequestParam("problemId") UUID problemId,
            @RequestParam("teamId") UUID teamId) {
        var command = new GetSubmissionHistoryCommand(problemId, teamId);
        List<Submission> submissions = getSubmissionHistoryUseCase.execute(command);
        List<SubmissionResponseDTO> response = submissions.stream()
                .map(SubmissionWebMapper::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint para obtener las métricas de evaluación de un envío.
     * <p>
     * Requiere rol {@code STUDENT}, {@code ADMIN}, {@code ORGANIZER} o {@code SERVICE}.
     * </p>
     *
     * @param id Identificador del envío.
     * @return 200 OK con las métricas del envío.
     */
    @GetMapping("/{id}/metrics")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN','ORGANIZER','SERVICE')")
    public ResponseEntity<SubmissionMetricsResponseDTO> getMetrics(@PathVariable("id") UUID id) {
        SubmissionMetrics metrics = getSubmissionMetricsUseCase.execute(id);
        return ResponseEntity.ok(SubmissionWebMapper.toResponse(metrics));
    }

    /**
     * Endpoint para eliminar un envío.
     * <p>
     * Requiere rol {@code ADMIN} u {@code ORGANIZER}.
     * </p>
     *
     * @param id Identificador del envío a eliminar.
     * @return 204 NO CONTENT.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZER')")
    public ResponseEntity<Void> delete(@PathVariable("id") UUID id) {
        deleteSubmissionUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }
}
