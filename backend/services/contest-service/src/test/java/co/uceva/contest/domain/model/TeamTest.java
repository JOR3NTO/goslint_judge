package co.uceva.contest.domain.model;

import co.uceva.contest.domain.exception.InvalidTeamException;
import co.uceva.contest.domain.exception.MaxTeamMembersReachedException;
import co.uceva.contest.domain.exception.MemberAlreadyInTeamException;
import co.uceva.contest.domain.exception.MemberNotFoundException;
import co.uceva.contest.domain.valueobject.TeamName;
import co.uceva.contest.domain.valueobject.TeamScore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Tests unitarios para la entidad Team")
class TeamTest {

    private static Team newTeam() {
        return Team.create(UUID.randomUUID(), TeamName.of("Equipo Test"));
    }

    @Test
    @DisplayName("Debe crear Team con factory method create(contestId, teamName)")
    void shouldCreateTeamWithFactoryMethod() {
        UUID contestId = UUID.randomUUID();
        TeamName name = TeamName.of("Los Campeones");

        Team team = Team.create(contestId, name);

        assertThat(team.getId()).isNotNull();
        assertThat(team.getContestId()).isEqualTo(contestId);
        assertThat(team.getTeamName()).isEqualTo(name);
        assertThat(team.getTeamScore()).isEqualTo(TeamScore.ZERO);
        assertThat(team.getCreatedAt()).isNotNull();
        assertThat(team.getMembers()).isEmpty();
    }

    @Test
    @DisplayName("Debe crear Team e incorporar al creador como primer integrante")
    void shouldCreateTeamWithCreatorAsMember() {
        UUID creatorId = UUID.randomUUID();

        Team team = Team.create(UUID.randomUUID(), TeamName.of("Equipo Alfa"), creatorId);

        assertThat(team.getMembers()).hasSize(1);
        assertThat(team.hasMember(creatorId)).isTrue();
        assertThat(team.getMemberUserIds()).containsExactly(creatorId);
    }

    @Test
    @DisplayName("Debe lanzar excepción al crear con TeamName nulo")
    void shouldThrowWhenTeamNameIsNull() {
        UUID contestId = UUID.randomUUID();

        assertThatThrownBy(() -> Team.create(contestId, null))
                .isInstanceOf(InvalidTeamException.class)
                .hasMessageContaining("El nombre del equipo no puede ser nulo.");

        assertThatThrownBy(() -> Team.create(contestId, null, UUID.randomUUID()))
                .isInstanceOf(InvalidTeamException.class)
                .hasMessageContaining("El nombre del equipo no puede ser nulo.");
    }

    @Test
    @DisplayName("Debe lanzar excepción si contestId es nulo")
    void shouldThrowWhenContestIdIsNull() {
        assertThatThrownBy(() -> Team.create(null, TeamName.of("Validos")))
                .isInstanceOf(InvalidTeamException.class)
                .hasMessageContaining("El identificador del concurso es obligatorio.");
    }

    @Test
    @DisplayName("Debe construir un equipo completo mediante Builder")
    void shouldBuildFullTeamWithBuilder() {
        UUID id = UUID.randomUUID();
        UUID contestId = UUID.randomUUID();
        Instant createdAt = Instant.now().minusSeconds(100);
        Member member = Member.of(id, UUID.randomUUID());

        Team team = Team.builder()
                .id(id)
                .contestId(contestId)
                .teamName(TeamName.of("Byte Masters"))
                .teamScore(TeamScore.of(4, 250L))
                .createdAt(createdAt)
                .members(List.of(member))
                .build();

        assertThat(team.getId()).isEqualTo(id);
        assertThat(team.getContestId()).isEqualTo(contestId);
        assertThat(team.getTeamName()).isEqualTo(TeamName.of("Byte Masters"));
        assertThat(team.getTeamScore()).isEqualTo(TeamScore.of(4, 250L));
        assertThat(team.getCreatedAt()).isEqualTo(createdAt);
        assertThat(team.getMembers()).containsExactly(member);
    }

    @Test
    @DisplayName("Debe actualizar el nombre del equipo")
    void shouldUpdateTeamName() {
        Team team = newTeam();

        team.updateName(TeamName.of("Otro Nombre"));

        assertThat(team.getTeamName()).isEqualTo(TeamName.of("Otro Nombre"));
    }

    @Test
    @DisplayName("Debe lanzar excepción al actualizar nombre con TeamName nulo")
    void shouldThrowWhenUpdatingWithNullTeamName() {
        Team team = newTeam();

        assertThatThrownBy(() -> team.updateName(null))
                .isInstanceOf(InvalidTeamException.class)
                .hasMessageContaining("El nombre del equipo no puede ser nulo.");
    }

    @Test
    @DisplayName("Debe agregar miembros respetando el límite por defecto de 3 integrantes")
    void shouldAddMembersUpToDefaultLimit() {
        Team team = newTeam();
        UUID u1 = UUID.randomUUID();
        UUID u2 = UUID.randomUUID();
        UUID u3 = UUID.randomUUID();

        Member added = team.addMember(u1);
        team.addMember(u2);
        team.addMember(u3);

        assertThat(added.getTeamId()).isEqualTo(team.getId());
        assertThat(team.getMembers()).hasSize(3);
        assertThat(team.hasMember(u1)).isTrue();
        assertThat(team.hasMember(u2)).isTrue();
        assertThat(team.hasMember(u3)).isTrue();

        assertThatThrownBy(() -> team.addMember(UUID.randomUUID()))
                .isInstanceOf(MaxTeamMembersReachedException.class)
                .hasMessageContaining("límite máximo de integrantes permitidos (3)");
    }

    @Test
    @DisplayName("Debe respetar un límite de integrantes personalizado")
    void shouldRespectCustomMemberLimit() {
        Team team = newTeam();
        team.addMember(UUID.randomUUID(), 1);

        assertThatThrownBy(() -> team.addMember(UUID.randomUUID(), 1))
                .isInstanceOf(MaxTeamMembersReachedException.class)
                .hasMessageContaining("(1)");
    }

    @Test
    @DisplayName("Debe fallar al agregar un userId nulo")
    void shouldThrowWhenAddingNullUserId() {
        Team team = newTeam();

        assertThatThrownBy(() -> team.addMember(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("El ID del usuario es obligatorio.");
    }

    @Test
    @DisplayName("Debe rechazar miembro duplicado")
    void shouldRejectDuplicateMember() {
        Team team = newTeam();
        UUID userId = UUID.randomUUID();

        team.addMember(userId);

        assertThatThrownBy(() -> team.addMember(userId))
                .isInstanceOf(MemberAlreadyInTeamException.class)
                .hasMessageContaining("ya pertenece al equipo");
    }

    @Test
    @DisplayName("Debe remover un miembro existente")
    void shouldRemoveExistingMember() {
        Team team = newTeam();
        UUID u1 = UUID.randomUUID();
        UUID u2 = UUID.randomUUID();
        team.addMember(u1);
        team.addMember(u2);

        team.removeMember(u1);

        assertThat(team.hasMember(u1)).isFalse();
        assertThat(team.hasMember(u2)).isTrue();
        assertThat(team.getMembers()).hasSize(1);
    }

    @Test
    @DisplayName("Debe lanzar excepción al remover miembro no existente o nulo")
    void shouldThrowWhenRemovingNonExistentMember() {
        Team team = newTeam();
        UUID nonExistent = UUID.randomUUID();

        assertThatThrownBy(() -> team.removeMember(nonExistent))
                .isInstanceOf(MemberNotFoundException.class)
                .hasMessageContaining("no pertenece al equipo");

        assertThatThrownBy(() -> team.removeMember(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("El ID del usuario es obligatorio.");
    }

    @Test
    @DisplayName("Debe retornar lista inmutable de integrantes")
    void shouldReturnUnmodifiableMembersList() {
        Team team = newTeam();
        Member member = Member.of(team.getId(), UUID.randomUUID());

        List<Member> members = team.getMembers();
        assertThatThrownBy(() -> members.add(member))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Debe acumular y reemplazar puntaje y penalización")
    void shouldAddAndReplaceScore() {
        Team team = newTeam();

        team.addScore(2, 40L);
        assertThat(team.getTeamScore()).isEqualTo(TeamScore.of(2, 40L));

        team.addScore(1, 20L);
        assertThat(team.getTeamScore()).isEqualTo(TeamScore.of(3, 60L));

        team.updateScore(TeamScore.of(6, 150L));
        assertThat(team.getTeamScore()).isEqualTo(TeamScore.of(6, 150L));
    }

    @Test
    @DisplayName("Debe lanzar excepción al actualizar puntaje con valores negativos o nulos")
    void shouldThrowWhenScoreIsInvalid() {
        Team team = newTeam();

        assertThatThrownBy(() -> team.addScore(-1, 10L))
                .isInstanceOf(InvalidTeamException.class)
                .hasMessageContaining("no negativos");

        assertThatThrownBy(() -> team.addScore(1, -10L))
                .isInstanceOf(InvalidTeamException.class)
                .hasMessageContaining("no negativos");

        assertThatThrownBy(() -> team.updateScore(null))
                .isInstanceOf(InvalidTeamException.class)
                .hasMessageContaining("El puntaje no puede ser nulo.");
    }
}
