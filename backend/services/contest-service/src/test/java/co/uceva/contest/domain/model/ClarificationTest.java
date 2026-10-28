package co.uceva.contest.domain.model;

import co.uceva.contest.domain.exception.InvalidClarificationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Tests unitarios para la entidad Clarification")
class ClarificationTest {

    private final UUID problemId = UUID.randomUUID();
    private final UUID teamId = UUID.randomUUID();

    private Clarification pending() {
        return Clarification.ask(problemId, teamId, "  ¿El límite incluye el 0?  ");
    }

    @Test
    @DisplayName("Debe crear la pregunta pendiente, sin respuesta y con el texto recortado")
    void shouldAskPending() {
        Clarification c = pending();

        assertThat(c.getId()).isNotNull();
        assertThat(c.getProblemId()).isEqualTo(problemId);
        assertThat(c.getTeamId()).isEqualTo(teamId);
        assertThat(c.getQuestion()).isEqualTo("¿El límite incluye el 0?");
        assertThat(c.getStatus()).isEqualTo(ClarificationStatus.PENDING);
        assertThat(c.isAnswered()).isFalse();
        assertThat(c.getAnswer()).isNull();
        assertThat(c.getAnsweredAt()).isNull();
        assertThat(c.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Debe exigir problema y equipo")
    void shouldRequireIds() {
        assertThatThrownBy(() -> Clarification.ask(null, teamId, "¿Hola?"))
                .isInstanceOf(InvalidClarificationException.class).hasMessageContaining("problema");
        assertThatThrownBy(() -> Clarification.ask(problemId, null, "¿Hola?"))
                .isInstanceOf(InvalidClarificationException.class).hasMessageContaining("equipo");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\n"})
    @DisplayName("Debe rechazar una pregunta vacía")
    void shouldRejectBlankQuestion(String blank) {
        assertThatThrownBy(() -> Clarification.ask(problemId, teamId, blank))
                .isInstanceOf(InvalidClarificationException.class).hasMessageContaining("pregunta");
        assertThatThrownBy(() -> Clarification.ask(problemId, teamId, null))
                .isInstanceOf(InvalidClarificationException.class);
    }

    @Test
    @DisplayName("Debe aceptar el texto en el límite y rechazar el que lo supera")
    void shouldLimitLength() {
        assertThat(Clarification.ask(problemId, teamId, "a".repeat(Clarification.MAX_TEXT_LENGTH)).getQuestion())
                .hasSize(Clarification.MAX_TEXT_LENGTH);
        assertThatThrownBy(() -> Clarification.ask(problemId, teamId, "a".repeat(Clarification.MAX_TEXT_LENGTH + 1)))
                .isInstanceOf(InvalidClarificationException.class).hasMessageContaining("2000");
    }

    @Test
    @DisplayName("Debe responderse una sola vez y registrar cuándo")
    void shouldAnswerOnce() {
        Clarification c = pending();

        c.answer("  Sí, incluye el 0.  ");

        assertThat(c.isAnswered()).isTrue();
        assertThat(c.getStatus()).isEqualTo(ClarificationStatus.ANSWERED);
        assertThat(c.getAnswer()).isEqualTo("Sí, incluye el 0.");
        assertThat(c.getAnsweredAt()).isNotNull();
        assertThatThrownBy(() -> c.answer("Otra respuesta"))
                .isInstanceOf(InvalidClarificationException.class).hasMessageContaining("ya fue respondida");
        assertThat(c.getAnswer()).isEqualTo("Sí, incluye el 0.");
    }

    @Test
    @DisplayName("Una respuesta inválida no cambia el estado")
    void shouldKeepPendingOnInvalidAnswer() {
        Clarification c = pending();

        assertThatThrownBy(() -> c.answer("  ")).isInstanceOf(InvalidClarificationException.class);
        assertThatThrownBy(() -> c.answer(null)).isInstanceOf(InvalidClarificationException.class);

        assertThat(c.isAnswered()).isFalse();
        assertThat(c.getAnswer()).isNull();
        assertThat(c.getAnsweredAt()).isNull();
    }

    @Test
    @DisplayName("Debe reconstruirse una aclaración respondida y rechazar estados inconsistentes")
    void shouldRestoreAndRejectInconsistentState() {
        Instant answeredAt = Instant.parse("2026-10-25T10:00:00Z");

        Clarification restored = Clarification.builder().id(UUID.randomUUID()).problemId(problemId).teamId(teamId)
                .question("¿Q?").answer("R").status(ClarificationStatus.ANSWERED).answeredAt(answeredAt).build();

        assertThat(restored.isAnswered()).isTrue();
        assertThat(restored.getAnsweredAt()).isEqualTo(answeredAt);
        assertThatThrownBy(() -> Clarification.builder().problemId(problemId).teamId(teamId).question("¿Q?")
                .status(ClarificationStatus.ANSWERED).build()).isInstanceOf(InvalidClarificationException.class);
        assertThatThrownBy(() -> Clarification.builder().problemId(problemId).teamId(teamId).question("¿Q?")
                .answer("R").build()).isInstanceOf(InvalidClarificationException.class);
    }
}
