package co.uceva.contest.infrastructure.web.controller;

import java.util.List;
import java.util.UUID;

import co.uceva.contest.application.usecase.*;
import co.uceva.contest.infrastructure.web.dto.*;
import co.uceva.contest.infrastructure.web.mapper.TeamWebMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST de equipos e integrantes. Delega toda la lógica en los casos de uso.
 * Crear, editar y gestionar integrantes exige estar autenticado; eliminar un equipo,
 * rol {@code ADMIN} u {@code ORGANIZER}; las consultas son públicas.
 */
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/v1")
public class TeamController {

    private final CreateTeamUseCase createTeamUseCase;
    private final GetTeamByIdUseCase getTeamByIdUseCase;
    private final GetAllTeamsUseCase getAllTeamsUseCase;
    private final GetTeamsByContestUseCase getTeamsByContestUseCase;
    private final GetTeamsByNameUseCase getTeamsByNameUseCase;
    private final GetTeamsByUserUseCase getTeamsByUserUseCase;
    private final GetScoreboardUseCase getScoreboardUseCase;
    private final UpdateTeamUseCase updateTeamUseCase;
    private final DeleteTeamUseCase deleteTeamUseCase;
    private final AddMemberUseCase addMemberUseCase;
    private final RemoveMemberUseCase removeMemberUseCase;
    private final GetMembersByTeamUseCase getMembersByTeamUseCase;

    public TeamController(
            CreateTeamUseCase createTeamUseCase,
            GetTeamByIdUseCase getTeamByIdUseCase,
            GetAllTeamsUseCase getAllTeamsUseCase,
            GetTeamsByContestUseCase getTeamsByContestUseCase,
            GetTeamsByNameUseCase getTeamsByNameUseCase,
            GetTeamsByUserUseCase getTeamsByUserUseCase,
            GetScoreboardUseCase getScoreboardUseCase,
            UpdateTeamUseCase updateTeamUseCase,
            DeleteTeamUseCase deleteTeamUseCase,
            AddMemberUseCase addMemberUseCase,
            RemoveMemberUseCase removeMemberUseCase,
            GetMembersByTeamUseCase getMembersByTeamUseCase) {
        this.createTeamUseCase = createTeamUseCase;
        this.getTeamByIdUseCase = getTeamByIdUseCase;
        this.getAllTeamsUseCase = getAllTeamsUseCase;
        this.getTeamsByContestUseCase = getTeamsByContestUseCase;
        this.getTeamsByNameUseCase = getTeamsByNameUseCase;
        this.getTeamsByUserUseCase = getTeamsByUserUseCase;
        this.getScoreboardUseCase = getScoreboardUseCase;
        this.updateTeamUseCase = updateTeamUseCase;
        this.deleteTeamUseCase = deleteTeamUseCase;
        this.addMemberUseCase = addMemberUseCase;
        this.removeMemberUseCase = removeMemberUseCase;
        this.getMembersByTeamUseCase = getMembersByTeamUseCase;
    }

    /** Crea un equipo en el concurso; el creador queda como primer integrante. 201 CREATED. */
    @PostMapping("/contests/{contestId}/teams")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TeamResponseDTO> create(
            @PathVariable("contestId") UUID contestId,
            @RequestBody CreateTeamRequestDTO request) {
        var created = createTeamUseCase.execute(TeamWebMapper.toCommand(contestId, request));
        return ResponseEntity.status(HttpStatus.CREATED).body(TeamWebMapper.toResponse(created));
    }

    /** Lista los equipos de un concurso; con {@code name} filtra por nombre. */
    @GetMapping("/contests/{contestId}/teams")
    public ResponseEntity<List<TeamResponseDTO>> getByContest(
            @PathVariable("contestId") UUID contestId,
            @RequestParam(value = "name", required = false) String name) {
        var teams = name == null
                ? getTeamsByContestUseCase.execute(contestId)
                : getTeamsByNameUseCase.execute(contestId, name);
        return ResponseEntity.ok(teams.stream().map(TeamWebMapper::toResponse).toList());
    }

    /** Tabla de posiciones del concurso, ordenada por puntaje y penalización. */
    @GetMapping("/contests/{contestId}/scoreboard")
    public ResponseEntity<List<TeamResponseDTO>> getScoreboard(@PathVariable("contestId") UUID contestId) {
        return ResponseEntity.ok(getScoreboardUseCase.execute(contestId).stream()
                .map(TeamWebMapper::toResponse).toList());
    }

    @GetMapping("/teams")
    public ResponseEntity<List<TeamResponseDTO>> getAll() {
        return ResponseEntity.ok(getAllTeamsUseCase.execute().stream().map(TeamWebMapper::toResponse).toList());
    }

    @GetMapping("/teams/{id}")
    public ResponseEntity<TeamResponseDTO> getById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(TeamWebMapper.toResponse(getTeamByIdUseCase.execute(id)));
    }

    @GetMapping("/teams/user/{userId}")
    public ResponseEntity<List<TeamResponseDTO>> getByUser(@PathVariable("userId") UUID userId) {
        return ResponseEntity.ok(getTeamsByUserUseCase.execute(userId).stream()
                .map(TeamWebMapper::toResponse).toList());
    }

    @PutMapping("/teams/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TeamResponseDTO> update(
            @PathVariable("id") UUID id,
            @RequestBody UpdateTeamRequestDTO request) {
        var updated = updateTeamUseCase.execute(TeamWebMapper.toCommand(id, request));
        return ResponseEntity.ok(TeamWebMapper.toResponse(updated));
    }

    @DeleteMapping("/teams/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZER')")
    public ResponseEntity<Void> delete(@PathVariable("id") UUID id) {
        deleteTeamUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/teams/{id}/members")
    public ResponseEntity<List<MemberResponseDTO>> getMembers(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(getMembersByTeamUseCase.execute(id).stream()
                .map(TeamWebMapper::toResponse).toList());
    }

    @PostMapping("/teams/{id}/members")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MemberResponseDTO> addMember(
            @PathVariable("id") UUID id,
            @RequestBody AddMemberRequestDTO request) {
        var member = addMemberUseCase.execute(TeamWebMapper.toCommand(id, request));
        return ResponseEntity.status(HttpStatus.CREATED).body(TeamWebMapper.toResponse(member));
    }

    @DeleteMapping("/teams/{id}/members/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> removeMember(@PathVariable("id") UUID id, @PathVariable("userId") UUID userId) {
        removeMemberUseCase.execute(id, userId);
        return ResponseEntity.noContent().build();
    }
}
