package co.uceva.contest.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Tests unitarios para la entidad Member")
class MemberTest {

    @Test
    @DisplayName("Debe crear Member usando factory method of(teamId, userId)")
    void shouldCreateMemberWithFactoryMethod() {
        UUID teamId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Member member = Member.of(teamId, userId);

        assertThat(member.getId()).isNotNull();
        assertThat(member.getTeamId()).isEqualTo(teamId);
        assertThat(member.getUserId()).isEqualTo(userId);
        assertThat(member.getRegisteredAt()).isNotNull();
    }

    @Test
    @DisplayName("Debe reconstruir Member con todos los atributos")
    void shouldRebuildMemberWithAllAttributes() {
        UUID id = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant registeredAt = Instant.now().minusSeconds(3600);

        Member member = Member.of(id, teamId, userId, registeredAt);

        assertThat(member.getId()).isEqualTo(id);
        assertThat(member.getTeamId()).isEqualTo(teamId);
        assertThat(member.getUserId()).isEqualTo(userId);
        assertThat(member.getRegisteredAt()).isEqualTo(registeredAt);
    }

    @Test
    @DisplayName("Debe permitir crear Member sin teamId inicial y asignarlo posteriormente")
    void shouldAllowMemberWithoutTeamIdInitially() {
        UUID userId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();

        Member member = Member.of(null, userId);
        assertThat(member.getTeamId()).isNull();

        member.setTeamId(teamId);
        assertThat(member.getTeamId()).isEqualTo(teamId);
    }

    @Test
    @DisplayName("Debe fallar al crear Member con userId nulo")
    void shouldThrowWhenUserIdIsNull() {
        UUID teamId = UUID.randomUUID();

        assertThatThrownBy(() -> Member.of(teamId, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("El ID del usuario es obligatorio.");
    }

    @Test
    @DisplayName("Dos miembros con mismo teamId y userId deben ser iguales según equals y hashCode")
    void shouldBeEqualWhenTeamIdAndUserIdMatch() {
        UUID teamId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Member m1 = Member.of(UUID.randomUUID(), teamId, userId, Instant.now());
        Member m2 = Member.of(UUID.randomUUID(), teamId, userId, Instant.now().minusSeconds(10));

        assertThat(m1).isEqualTo(m2);
        assertThat(m1.hashCode()).isEqualTo(m2.hashCode());
    }

    @Test
    @DisplayName("ToString debe contener información relevante")
    void shouldHaveInformativeToString() {
        UUID teamId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Member member = Member.of(teamId, userId);

        assertThat(member.toString()).contains(teamId.toString(), userId.toString());
    }
}
