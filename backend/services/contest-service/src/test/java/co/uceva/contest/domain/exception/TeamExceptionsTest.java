package co.uceva.contest.domain.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Tests unitarios para las excepciones de dominio de Team")
class TeamExceptionsTest {

    @Test
    @DisplayName("TeamNotFoundException debe contener el id o mensaje personalizado")
    void shouldFormatTeamNotFoundException() {
        UUID teamId = UUID.randomUUID();
        TeamNotFoundException ex1 = new TeamNotFoundException(teamId);
        TeamNotFoundException ex2 = new TeamNotFoundException("Equipo no encontrado.");

        assertThat(ex1.getMessage()).contains(teamId.toString());
        assertThat(ex2.getMessage()).isEqualTo("Equipo no encontrado.");
    }

    @Test
    @DisplayName("ExistingTeamException debe formatear con id, con nombre y contestId, o mensaje personalizado")
    void shouldFormatExistingTeamException() {
        UUID teamId = UUID.randomUUID();
        UUID contestId = UUID.randomUUID();

        ExistingTeamException ex1 = new ExistingTeamException(teamId);
        ExistingTeamException ex2 = new ExistingTeamException("Equipo Alfa", contestId);
        ExistingTeamException ex3 = new ExistingTeamException("Mensaje personalizado.");

        assertThat(ex1.getMessage()).contains(teamId.toString());
        assertThat(ex2.getMessage()).contains("Equipo Alfa", contestId.toString());
        assertThat(ex3.getMessage()).isEqualTo("Mensaje personalizado.");
    }

    @Test
    @DisplayName("NoTeamsException debe tener mensaje por defecto o personalizado")
    void shouldFormatNoTeamsException() {
        NoTeamsException ex1 = new NoTeamsException();
        NoTeamsException ex2 = new NoTeamsException("No hay equipos en la BD.");

        assertThat(ex1.getMessage()).isEqualTo("No hay equipos.");
        assertThat(ex2.getMessage()).isEqualTo("No hay equipos en la BD.");
    }

    @Test
    @DisplayName("MemberNotFoundException debe formatear con userId o con userId y teamId")
    void shouldFormatMemberNotFoundException() {
        UUID userId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();

        MemberNotFoundException ex1 = new MemberNotFoundException(userId);
        MemberNotFoundException ex2 = new MemberNotFoundException(userId, teamId);

        assertThat(ex1.getMessage()).contains(userId.toString());
        assertThat(ex2.getMessage()).contains(userId.toString(), teamId.toString());
    }

    @Test
    @DisplayName("MemberAlreadyInTeamException debe formatear con userId o con userId y contestId")
    void shouldFormatMemberAlreadyInTeamException() {
        UUID userId = UUID.randomUUID();
        UUID contestId = UUID.randomUUID();

        MemberAlreadyInTeamException ex1 = new MemberAlreadyInTeamException(userId);
        MemberAlreadyInTeamException ex2 = new MemberAlreadyInTeamException(userId, contestId);

        assertThat(ex1.getMessage()).contains(userId.toString());
        assertThat(ex2.getMessage()).contains(userId.toString(), contestId.toString());
    }

    @Test
    @DisplayName("MaxTeamMembersReachedException debe formatear con la cantidad máxima")
    void shouldFormatMaxTeamMembersReachedException() {
        MaxTeamMembersReachedException ex = new MaxTeamMembersReachedException(3);

        assertThat(ex.getMessage()).contains("3");
    }

    @Test
    @DisplayName("InvalidTeamException debe preservar el mensaje de validación")
    void shouldFormatInvalidTeamException() {
        InvalidTeamException ex = new InvalidTeamException("El nombre es inválido.");

        assertThat(ex.getMessage()).isEqualTo("El nombre es inválido.");
    }
}

