package co.uceva.contest.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import co.uceva.contest.domain.model.Team;
import co.uceva.contest.domain.repository.TeamRepository;
import co.uceva.contest.infrastructure.mapper.TeamEntityMapper;
import co.uceva.contest.infrastructure.persistence.repository.SpringDataTeamRepository;
import org.springframework.stereotype.Component;

/**
 * Adaptador de infraestructura que implementa el puerto de salida {@link TeamRepository}.
 * Los integrantes se persisten junto con el equipo.
 */
@Component
public class TeamRepositoryImpl implements TeamRepository {

    private final SpringDataTeamRepository springDataRepository;

    public TeamRepositoryImpl(SpringDataTeamRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    /** {@inheritDoc} */
    @Override
    public Team save(Team team) {
        return TeamEntityMapper.toDomain(springDataRepository.save(TeamEntityMapper.toEntity(team)));
    }

    /** {@inheritDoc} */
    @Override
    public Optional<Team> findById(UUID teamId) {
        return springDataRepository.findById(teamId).map(TeamEntityMapper::toDomain);
    }

    /** {@inheritDoc} */
    @Override
    public void deleteById(UUID teamId) {
        springDataRepository.deleteById(teamId);
    }

    /** {@inheritDoc} */
    @Override
    public List<Team> findAll() {
        return toDomain(springDataRepository.findAll());
    }

    /** {@inheritDoc} */
    @Override
    public List<Team> findAllByContestId(UUID contestId) {
        return toDomain(springDataRepository.findByContestId(contestId));
    }

    /** {@inheritDoc} */
    @Override
    public List<Team> findAllByContestIdOrderByScoreDescPenaltyTimeAsc(UUID contestId) {
        return toDomain(springDataRepository.findByContestIdOrderByScoreDescPenaltyTimeAsc(contestId));
    }

    /** {@inheritDoc} */
    @Override
    public List<Team> findAllByContestIdAndNameTeamContaining(UUID contestId, String nameTeam) {
        return toDomain(springDataRepository.findByContestIdAndTeamNameContainingIgnoreCase(contestId, nameTeam));
    }

    /** {@inheritDoc} */
    @Override
    public Optional<Team> findByContestIdAndNameTeam(UUID contestId, String nameTeam) {
        return springDataRepository.findByContestIdAndTeamName(contestId, nameTeam).map(TeamEntityMapper::toDomain);
    }

    /** {@inheritDoc} */
    @Override
    public boolean existsByContestIdAndNameTeam(UUID contestId, String nameTeam) {
        return springDataRepository.existsByContestIdAndTeamName(contestId, nameTeam);
    }

    /** {@inheritDoc} */
    @Override
    public boolean existsById(UUID teamId) {
        return springDataRepository.existsById(teamId);
    }

    /** {@inheritDoc} */
    @Override
    public List<Team> findAllByUserId(UUID userId) {
        return toDomain(springDataRepository.findByUserId(userId));
    }

    /** {@inheritDoc} */
    @Override
    public boolean existsByContestIdAndUserId(UUID contestId, UUID userId) {
        return springDataRepository.existsByContestIdAndUserId(contestId, userId);
    }

    /** {@inheritDoc} */
    @Override
    public Optional<Team> findByContestIdAndUserId(UUID contestId, UUID userId) {
        return springDataRepository.findByContestIdAndUserId(contestId, userId).map(TeamEntityMapper::toDomain);
    }

    private static List<Team> toDomain(List<co.uceva.contest.infrastructure.persistence.entity.TeamEntity> entities) {
        return entities.stream().map(TeamEntityMapper::toDomain).toList();
    }
}
