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

import co.uceva.contest.application.usecase.AddProblemToContestUseCase.AddProblemToContestCommand;
import co.uceva.contest.application.usecase.UpdateContestProblemUseCase.UpdateContestProblemCommand;
import co.uceva.contest.application.usecase.impl.AddProblemToContestUseCaseImpl;
import co.uceva.contest.application.usecase.impl.GetProblemsByContestUseCaseImpl;
import co.uceva.contest.application.usecase.impl.RemoveProblemFromContestUseCaseImpl;
import co.uceva.contest.application.usecase.impl.UpdateContestProblemUseCaseImpl;
import co.uceva.contest.domain.exception.ContestNotFoundException;
import co.uceva.contest.domain.exception.ContestProblemNotFoundException;
import co.uceva.contest.domain.exception.ExistingContestProblemException;
import co.uceva.contest.domain.model.Contest;
import co.uceva.contest.domain.model.ContestProblem;
import co.uceva.contest.domain.repository.ContestProblemRepository;
import co.uceva.contest.domain.exception.ProblemNotFoundException;
import co.uceva.contest.domain.repository.ContestRepository;
import co.uceva.contest.domain.service.ProblemCatalog;
import co.uceva.contest.domain.valueobject.BalloonColor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Problemas de concurso: asignación, actualización, retiro y consulta")
class ContestProblemUseCasesTest {

    private final ContestProblemRepository problems = mock(ContestProblemRepository.class);
    private final ContestRepository contests = mock(ContestRepository.class);
    private final ProblemCatalog catalog = mock(ProblemCatalog.class);
    private final AddProblemToContestUseCaseImpl addUseCase = new AddProblemToContestUseCaseImpl(problems, contests, catalog);

    private final UUID contestId = UUID.randomUUID();
    private final UUID problemId = UUID.randomUUID();

    @Test
    @DisplayName("Rechaza un problema que no existe en el catálogo")
    void addRejectsUnknownProblem() {
        givenContest();
        when(catalog.exists(problemId)).thenReturn(false);

        assertThatThrownBy(() -> addUseCase.execute(addCommand("A")))
                .isInstanceOf(ProblemNotFoundException.class);
        verify(problems, never()).save(any());
    }

    private void givenContest() {
        when(contests.findById(contestId)).thenReturn(Optional.of(Contest.builder().id(contestId).build()));
    }

    private AddProblemToContestCommand addCommand(String alias) {
        return new AddProblemToContestCommand(contestId, problemId, alias, 0, 1, "#ff0000");
    }

    @Test
    @DisplayName("Asigna el problema normalizando alias y color")
    void addsProblem() {
        givenContest();
        when(catalog.exists(problemId)).thenReturn(true);
        when(problems.save(any(ContestProblem.class))).thenAnswer(i -> i.getArgument(0));

        ContestProblem saved = addUseCase.execute(addCommand("a"));

        assertThat(saved.getAlias()).isEqualTo("A");
        assertThat(saved.getBalloonColor()).isEqualTo(BalloonColor.of("#FF0000"));
        verify(problems).save(saved);
    }

    @Test
    @DisplayName("Rechaza un concurso inexistente")
    void addRejectsUnknownContest() {
        when(contests.findById(contestId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> addUseCase.execute(addCommand("A")))
                .isInstanceOf(ContestNotFoundException.class);
        verify(problems, never()).save(any());
    }

    @Test
    @DisplayName("Rechaza un problema repetido y un alias repetido (sin distinguir mayúsculas)")
    void addRejectsDuplicates() {
        givenContest();
        var useCase = addUseCase;

        when(problems.existsByContestIdAndProblemId(contestId, problemId)).thenReturn(true);
        assertThatThrownBy(() -> useCase.execute(addCommand("A")))
                .isInstanceOf(ExistingContestProblemException.class);

        when(problems.existsByContestIdAndProblemId(contestId, problemId)).thenReturn(false);
        when(problems.existsByContestIdAndAlias(contestId, "A")).thenReturn(true);
        assertThatThrownBy(() -> useCase.execute(addCommand("a")))
                .isInstanceOf(ExistingContestProblemException.class)
                .hasMessageContaining("alias");
        verify(problems, never()).save(any());
    }

    @Test
    @DisplayName("Actualiza el problema y solo valida el alias si cambió")
    void updatesProblem() {
        ContestProblem existing = ContestProblem.create(contestId, problemId, "A", 0, BalloonColor.of("#FF0000"));
        when(problems.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(problems.save(existing)).thenReturn(existing);
        var useCase = new UpdateContestProblemUseCaseImpl(problems);

        useCase.execute(new UpdateContestProblemCommand(existing.getId(), "a", 2, 3, "#00ff00"));

        assertThat(existing.getOrderIndex()).isEqualTo(2);
        assertThat(existing.getPoints()).isEqualTo(3);
        assertThat(existing.getBalloonColor().value()).isEqualTo("#00FF00");
        verify(problems, never()).existsByContestIdAndAlias(any(), any());
    }

    @Test
    @DisplayName("Rechaza actualizar a un alias ya usado o un problema inexistente")
    void updateRejects() {
        ContestProblem existing = ContestProblem.create(contestId, problemId, "A", 0, BalloonColor.of("#FF0000"));
        when(problems.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(problems.existsByContestIdAndAlias(contestId, "B")).thenReturn(true);
        var useCase = new UpdateContestProblemUseCaseImpl(problems);

        assertThatThrownBy(() -> useCase.execute(
                new UpdateContestProblemCommand(existing.getId(), "b", 0, 1, "#FF0000")))
                .isInstanceOf(ExistingContestProblemException.class);
        verify(problems, never()).save(any());

        UUID unknown = UUID.randomUUID();
        when(problems.findById(unknown)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.execute(
                new UpdateContestProblemCommand(unknown, "A", 0, 1, "#FF0000")))
                .isInstanceOf(ContestProblemNotFoundException.class);
    }

    @Test
    @DisplayName("Retira el problema si existe y falla si no")
    void removesProblem() {
        ContestProblem existing = ContestProblem.create(contestId, problemId, "A", 0, BalloonColor.of("#FF0000"));
        when(problems.findById(existing.getId())).thenReturn(Optional.of(existing));
        var useCase = new RemoveProblemFromContestUseCaseImpl(problems);

        useCase.execute(existing.getId());
        verify(problems).deleteById(existing.getId());

        UUID unknown = UUID.randomUUID();
        when(problems.findById(unknown)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.execute(unknown)).isInstanceOf(ContestProblemNotFoundException.class);
        verify(problems, never()).deleteById(unknown);
    }

    @Test
    @DisplayName("Lista los problemas en orden y falla si el concurso no existe")
    void listsProblems() {
        givenContest();
        ContestProblem p = ContestProblem.create(contestId, problemId, "A", 0, BalloonColor.of("#FF0000"));
        when(problems.findAllByContestIdOrderByOrderIndexAsc(contestId)).thenReturn(List.of(p));
        var useCase = new GetProblemsByContestUseCaseImpl(problems, contests);

        assertThat(useCase.execute(contestId)).containsExactly(p);
        assertThatThrownBy(() -> useCase.execute(UUID.randomUUID())).isInstanceOf(ContestNotFoundException.class);
    }
}
