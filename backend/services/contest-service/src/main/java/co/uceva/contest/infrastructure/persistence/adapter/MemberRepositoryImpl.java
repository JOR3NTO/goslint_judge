package co.uceva.contest.infrastructure.persistence.adapter;

import java.util.List;
import java.util.UUID;

import co.uceva.contest.domain.model.Member;
import co.uceva.contest.domain.repository.MemberRepository;
import co.uceva.contest.infrastructure.mapper.MemberEntityMapper;
import co.uceva.contest.infrastructure.persistence.repository.SpringDataMemberRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adaptador de infraestructura que implementa el puerto de salida {@link MemberRepository}.
 */
@Component
public class MemberRepositoryImpl implements MemberRepository {

    private final SpringDataMemberRepository springDataRepository;

    public MemberRepositoryImpl(SpringDataMemberRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    /** {@inheritDoc} */
    @Override
    public Member save(Member member) {
        return MemberEntityMapper.toDomain(springDataRepository.save(MemberEntityMapper.toEntity(member)));
    }

    /** {@inheritDoc} */
    @Override
    public List<Member> saveAll(List<Member> members) {
        return springDataRepository.saveAll(members.stream().map(MemberEntityMapper::toEntity).toList())
                .stream().map(MemberEntityMapper::toDomain).toList();
    }

    /** {@inheritDoc} */
    @Override
    public List<Member> findAllByTeamId(UUID teamId) {
        return springDataRepository.findByTeamId(teamId).stream().map(MemberEntityMapper::toDomain).toList();
    }

    /** {@inheritDoc} */
    @Override
    public List<Member> findAllByUserId(UUID userId) {
        return springDataRepository.findByUserId(userId).stream().map(MemberEntityMapper::toDomain).toList();
    }

    /** {@inheritDoc} */
    @Override
    public boolean existsByTeamIdAndUserId(UUID teamId, UUID userId) {
        return springDataRepository.existsByTeamIdAndUserId(teamId, userId);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void deleteByTeamIdAndUserId(UUID teamId, UUID userId) {
        springDataRepository.deleteByTeamIdAndUserId(teamId, userId);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void deleteAllByTeamId(UUID teamId) {
        springDataRepository.deleteByTeamId(teamId);
    }
}
