package co.uceva.contest.infrastructure.web.mapper;

import java.util.UUID;

import co.uceva.contest.application.usecase.AddMemberUseCase.AddMemberCommand;
import co.uceva.contest.application.usecase.CreateTeamUseCase.CreateTeamCommand;
import co.uceva.contest.application.usecase.UpdateTeamUseCase.UpdateTeamCommand;
import co.uceva.contest.domain.model.Member;
import co.uceva.contest.domain.model.Team;
import co.uceva.contest.infrastructure.web.dto.AddMemberRequestDTO;
import co.uceva.contest.infrastructure.web.dto.CreateTeamRequestDTO;
import co.uceva.contest.infrastructure.web.dto.MemberResponseDTO;
import co.uceva.contest.infrastructure.web.dto.TeamResponseDTO;
import co.uceva.contest.infrastructure.web.dto.UpdateTeamRequestDTO;

/**
 * Mapper entre DTOs de la capa web y comandos/entidades de aplicación y dominio
 * para equipos e integrantes.
 */
public class TeamWebMapper {

    public static CreateTeamCommand toCommand(UUID contestId, CreateTeamRequestDTO request) {
        return new CreateTeamCommand(contestId, request.teamName(), request.creatorUserId(), request.contestPassword());
    }

    public static UpdateTeamCommand toCommand(UUID teamId, UpdateTeamRequestDTO request) {
        return new UpdateTeamCommand(teamId, request.teamName());
    }

    public static AddMemberCommand toCommand(UUID teamId, AddMemberRequestDTO request) {
        return new AddMemberCommand(teamId, request.userId());
    }

    public static TeamResponseDTO toResponse(Team domain) {
        return new TeamResponseDTO(
                domain.getId(),
                domain.getContestId(),
                domain.getTeamName().value(),
                domain.getTeamScore().score(),
                domain.getTeamScore().penaltyTime(),
                domain.getCreatedAt(),
                domain.getMembers().stream().map(TeamWebMapper::toResponse).toList()
        );
    }

    public static MemberResponseDTO toResponse(Member domain) {
        return new MemberResponseDTO(domain.getId(), domain.getTeamId(), domain.getUserId(), domain.getRegisteredAt());
    }
}
