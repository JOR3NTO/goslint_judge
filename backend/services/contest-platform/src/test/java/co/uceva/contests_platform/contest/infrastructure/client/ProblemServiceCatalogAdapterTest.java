package co.uceva.contests_platform.contest.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.UUID;

import co.uceva.contests_platform.problem.application.usecase.GetProblemByIdUseCase;
import co.uceva.contests_platform.problem.domain.exception.ProblemNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Adaptador del catálogo de problemas sobre el caso de uso de problemas")
class ProblemServiceCatalogAdapterTest {

    private final UUID id = UUID.randomUUID();

    @Mock
    private GetProblemByIdUseCase getProblemByIdUseCase;

    @InjectMocks
    private ProblemServiceCatalogAdapter adapter;

    @Test
    @DisplayName("Si el caso de uso encuentra el problema, existe")
    void returnsTrueWhenProblemIsFound() {
        when(getProblemByIdUseCase.execute(id)).thenReturn(null);

        assertThat(adapter.exists(id)).isTrue();
    }

    @Test
    @DisplayName("Un problema inexistente se informa como ausente")
    void returnsFalseWhenProblemDoesNotExist() {
        when(getProblemByIdUseCase.execute(id)).thenThrow(new ProblemNotFoundException(id));

        assertThat(adapter.exists(id)).isFalse();
    }

    @Test
    @DisplayName("Un fallo inesperado no se confunde con un problema inexistente")
    void propagatesUnexpectedFailures() {
        when(getProblemByIdUseCase.execute(id)).thenThrow(new IllegalStateException("fallo"));

        assertThatThrownBy(() -> adapter.exists(id)).isInstanceOf(IllegalStateException.class);
    }
}
