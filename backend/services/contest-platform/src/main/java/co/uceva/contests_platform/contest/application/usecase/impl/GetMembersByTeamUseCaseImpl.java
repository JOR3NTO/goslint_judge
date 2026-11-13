package co.uceva.contests_platform.contest.application.usecase.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.uceva.contests_platform.contest.application.usecase.GetMembersByTeamUseCase;
import co.uceva.contests_platform.contest.domain.model.Member;
import co.uceva.contests_platform.contest.domain.repository.MemberRepository;

/**
 * Servicio de Aplicación que implementa el caso de uso: consultar los integrantes de un equipo.
 */
@Service
public class GetMembersByTeamUseCaseImpl implements GetMembersByTeamUseCase {

    private final MemberRepository memberRepository;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param memberRepository Puerto de salida para consultar integrantes.
     */
    public GetMembersByTeamUseCaseImpl(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    /**
     * Consultar los integrantes de un equipo.
     *
     * @param teamId Identificador del equipo.
     * @return Lista de resultados.
     */
    @Override
    @Transactional(readOnly = true)
    public List<Member> execute(UUID teamId) {
        return memberRepository.findAllByTeamId(teamId);
    }
}
