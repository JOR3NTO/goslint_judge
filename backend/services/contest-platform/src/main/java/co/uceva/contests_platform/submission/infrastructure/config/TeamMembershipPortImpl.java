package co.uceva.contests_platform.submission.infrastructure.config;

import co.uceva.contests_platform.contest.application.usecase.GetTeamByIdUseCase;
import co.uceva.contests_platform.contest.domain.model.Team;
import co.uceva.contests_platform.submission.application.port.out.TeamDTO;
import co.uceva.contests_platform.submission.application.port.out.TeamMembershipPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TeamMembershipPortImpl implements TeamMembershipPort {
    private final GetTeamByIdUseCase getTeamByIdUseCase;

    public TeamMembershipPortImpl(GetTeamByIdUseCase getTeamByIdUseCase){
        this.getTeamByIdUseCase = getTeamByIdUseCase;
    }

    @Override
    public TeamDTO findTeam(UUID teamId) {
        Team team = getTeamByIdUseCase.execute(teamId);
        return new TeamDTO(team.getId(), team.getMemberUserIds());
    }
}
