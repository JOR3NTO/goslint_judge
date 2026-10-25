package co.uceva.contest.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import co.uceva.contest.application.usecase.AnswerClarificationUseCase.AnswerClarificationCommand;
import co.uceva.contest.application.usecase.AskClarificationUseCase.AskClarificationCommand;
import co.uceva.contest.application.usecase.impl.AnswerClarificationUseCaseImpl;
import co.uceva.contest.application.usecase.impl.AskClarificationUseCaseImpl;
import co.uceva.contest.application.usecase.impl.GetClarificationsByContestUseCaseImpl;
import co.uceva.contest.domain.exception.ClarificationNotFoundException;
import co.uceva.contest.domain.exception.ContestNotFoundException;
import co.uceva.contest.domain.exception.ContestProblemNotFoundException;
import co.uceva.contest.domain.exception.InvalidClarificationException;
import co.uceva.contest.domain.exception.TeamNotFoundException;
import co.uceva.contest.domain.model.Clarification;
import co.uceva.contest.domain.model.ClarificationStatus;
import co.uceva.contest.domain.model.Contest;
import co.uceva.contest.domain.model.ContestProblem;
import co.uceva.contest.domain.model.Team;
import co.uceva.contest.domain.repository.ClarificationRepository;
import co.uceva.contest.domain.repository.ContestProblemRepository;
import co.uceva.contest.domain.repository.ContestRepository;
import co.uceva.contest.domain.repository.TeamRepository;
import co.uceva.contest.domain.valueobject.BalloonColor;
import co.uceva.contest.domain.valueobject.TeamName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Aclaraciones: preguntar, responder y consultar (públicas)")
class ClarificationUseCasesTest {

    private final ClarificationRepository clarifications = mock(ClarificationRepository.class);
    private final ContestProblemRepository problems = mock(ContestProblemRepository.class);
    private final TeamRepository teams = mock(TeamRepository.class);
    private final ContestRepository contests = mock(ContestRepository.class);

    private final UUID contestId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final ContestProblem problem =
            ContestProblem.create(contestId, UUID.randomUUID(), "A", 0, BalloonColor.of("#FF0000"));
    private final Team team = Team.create(contestId, TeamName.of("Equipo Test"), userId);

    private final AskClarificationUseCaseImpl ask = new AskClarificationUseCaseImpl(clarifications, problems, teams);

    private void givenSavesReturnArgument() {
        when(clarifications.save(any(Clarification.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    @DisplayName("Pregunta a nombre del equipo del usuario en el concurso del problema")
    void asksOnBehalfOfTeam() {
        when(problems.findById(problem.getId())).thenReturn(Optional.of(problem));
        when(teams.findByContestIdAndUserId(contestId, userId)).thenReturn(Optional.of(team));
        givenSavesReturnArgument();

        Clarification saved = ask.execute(new AskClarificationCommand(problem.getId(), userId, "¿Hay empates?"));

        assertThat(saved.getProblemId()).isEqualTo(problem.getId());
        assertThat(saved.getTeamId()).isEqualTo(team.getId());
        assertThat(saved.getStatus()).isEqualTo(ClarificationStatus.PENDING);
        verify(clarifications).save(saved);
    }

    @Test
    @DisplayName("No deja preguntar sobre un problema inexistente ni a quien no tiene equipo en el concurso")
    void askRejects() {
        UUID unknown = UUID.randomUUID();
        when(problems.findById(unknown)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> ask.execute(new AskClarificationCommand(unknown, userId, "¿Q?")))
                .isInstanceOf(ContestProblemNotFoundException.class);

        when(problems.findById(problem.getId())).thenReturn(Optional.of(problem));
        when(teams.findByContestIdAndUserId(contestId, userId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> ask.execute(new AskClarificationCommand(problem.getId(), userId, "¿Q?")))
                .isInstanceOf(TeamNotFoundException.class).hasMessageContaining("no pertenece");

        when(teams.findByContestIdAndUserId(contestId, userId)).thenReturn(Optional.of(team));
        assertThatThrownBy(() -> ask.execute(new AskClarificationCommand(problem.getId(), userId, "  ")))
                .isInstanceOf(InvalidClarificationException.class);
        verify(clarifications, never()).save(any());
    }

    @Test
    @DisplayName("Responde una aclaración pendiente una sola vez")
    void answersOnce() {
        Clarification pending = Clarification.ask(problem.getId(), team.getId(), "¿Hay empates?");
        when(clarifications.findById(pending.getId())).thenReturn(Optional.of(pending));
        givenSavesReturnArgument();
        var answer = new AnswerClarificationUseCaseImpl(clarifications);

        Clarification answered = answer.execute(new AnswerClarificationCommand(pending.getId(), "No."));

        assertThat(answered.isAnswered()).isTrue();
        assertThat(answered.getAnswer()).isEqualTo("No.");
        verify(clarifications).save(pending);
        assertThatThrownBy(() -> answer.execute(new AnswerClarificationCommand(pending.getId(), "Otra")))
                .isInstanceOf(InvalidClarificationException.class);

        UUID unknown = UUID.randomUUID();
        when(clarifications.findById(unknown)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> answer.execute(new AnswerClarificationCommand(unknown, "No.")))
                .isInstanceOf(ClarificationNotFoundException.class);
    }

    @Test
    @DisplayName("Lista las del concurso y falla si el concurso no existe")
    void listsByContest() {
        when(contests.findById(contestId)).thenReturn(Optional.of(Contest.builder().id(contestId).build()));
        Clarification c = Clarification.ask(problem.getId(), team.getId(), "¿Q?");
        when(clarifications.findAllByContestId(contestId)).thenReturn(List.of(c));
        var useCase = new GetClarificationsByContestUseCaseImpl(clarifications, contests);

        assertThat(useCase.execute(contestId)).containsExactly(c);
        assertThatThrownBy(() -> useCase.execute(UUID.randomUUID())).isInstanceOf(ContestNotFoundException.class);
    }
}
