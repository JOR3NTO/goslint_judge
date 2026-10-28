package co.uceva.contest.infrastructure.web.controller;

import java.util.List;
import java.util.UUID;

import co.uceva.contest.application.usecase.*;
import co.uceva.contest.infrastructure.web.dto.*;
import co.uceva.contest.infrastructure.web.mapper.ClarificationWebMapper;
import co.uceva.shared.infrastructure.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST de las aclaraciones de un concurso. Delega toda la lógica en los casos de uso.
 * Preguntar exige estar autenticado y pertenecer a un equipo del concurso; responder, rol
 * {@code ADMIN} u {@code ORGANIZER}; la consulta es pública porque las aclaraciones son públicas.
 */
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/v1")
public class ClarificationController {

    private final AskClarificationUseCase askClarificationUseCase;
    private final AnswerClarificationUseCase answerClarificationUseCase;
    private final GetClarificationsByContestUseCase getClarificationsByContestUseCase;

    /**
     * Inyección de dependencias para obtener los orquestadores de cada caso de uso.
     *
     * @param askClarificationUseCase           Caso de uso de envío de preguntas.
     * @param answerClarificationUseCase        Caso de uso de respuesta de aclaraciones.
     * @param getClarificationsByContestUseCase Caso de uso de consulta de las aclaraciones de un concurso.
     */
    public ClarificationController(
            AskClarificationUseCase askClarificationUseCase,
            AnswerClarificationUseCase answerClarificationUseCase,
            GetClarificationsByContestUseCase getClarificationsByContestUseCase) {
        this.askClarificationUseCase = askClarificationUseCase;
        this.answerClarificationUseCase = answerClarificationUseCase;
        this.getClarificationsByContestUseCase = getClarificationsByContestUseCase;
    }

    /**
     * Envía una pregunta sobre un problema del concurso, en nombre del equipo del usuario autenticado.
     *
     * @param problemId Identificador del problema del concurso ({@code ContestProblem}), no el de problem-service.
     * @param user      Usuario autenticado; debe pertenecer a un equipo del concurso del problema.
     * @param request   Texto de la pregunta.
     * @return 201 CREATED con la aclaración pendiente; 400 si la pregunta es inválida; 404 si el problema no
     *         existe o el usuario no tiene equipo en el concurso.
     */
    @PostMapping("/contest-problems/{problemId}/clarifications")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ClarificationResponseDTO> ask(
            @PathVariable("problemId") UUID problemId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody AskClarificationRequestDTO request) {
        var created = askClarificationUseCase.execute(ClarificationWebMapper.toCommand(problemId, user.userId(), request));
        return ResponseEntity.status(HttpStatus.CREATED).body(ClarificationWebMapper.toResponse(created));
    }

    /**
     * Lista las aclaraciones del concurso, de la más reciente a la más antigua.
     * <p>
     * Acceso público.
     * </p>
     *
     * @param contestId Identificador del concurso.
     * @return 200 OK con la lista (vacía si no hay); 404 si el concurso no existe.
     */
    @GetMapping("/contests/{contestId}/clarifications")
    public ResponseEntity<List<ClarificationResponseDTO>> getByContest(@PathVariable("contestId") UUID contestId) {
        return ResponseEntity.ok(getClarificationsByContestUseCase.execute(contestId).stream()
                .map(ClarificationWebMapper::toResponse).toList());
    }

    /**
     * Responde una aclaración pendiente.
     * <p>
     * Solo {@code ADMIN} u {@code ORGANIZER}. Una aclaración se responde una sola vez.
     * </p>
     *
     * @param id      Identificador de la aclaración.
     * @param request Texto de la respuesta.
     * @return 200 OK con la aclaración respondida; 400 si la respuesta es inválida; 404 si la aclaración no
     *         existe; 409 si ya fue respondida.
     */
    @PostMapping("/clarifications/{id}/answer")
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZER')")
    public ResponseEntity<ClarificationResponseDTO> answer(
            @PathVariable("id") UUID id,
            @RequestBody AnswerClarificationRequestDTO request) {
        var answered = answerClarificationUseCase.execute(ClarificationWebMapper.toCommand(id, request));
        return ResponseEntity.ok(ClarificationWebMapper.toResponse(answered));
    }
}
