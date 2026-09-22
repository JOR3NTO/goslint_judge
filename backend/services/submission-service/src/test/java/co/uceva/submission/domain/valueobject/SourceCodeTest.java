package co.uceva.submission.domain.valueobject;

import co.uceva.shared.domain.ProgrammingLanguage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SourceCodeTest {

    @Test
    void shouldCreateValidSourceCode() {
        SourceCode sourceCode = new SourceCode("print('hello')", ProgrammingLanguage.PYTHON);

        assertThat(sourceCode.content()).isEqualTo("print('hello')");
        assertThat(sourceCode.language()).isEqualTo(ProgrammingLanguage.PYTHON);
        assertThat(sourceCode.sizeInBytes()).isGreaterThan(0);
    }

    @Test
    void shouldRejectNullLanguage() {
        assertThatThrownBy(() -> new SourceCode("code", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lenguaje de programación");
    }

    @Test
    void shouldRejectBlankContent() {
        assertThatThrownBy(() -> new SourceCode("   ", ProgrammingLanguage.C))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("código fuente no puede estar vacío");
    }

    @Test
    void shouldRejectNullContent() {
        assertThatThrownBy(() -> new SourceCode(null, ProgrammingLanguage.JAVA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("código fuente no puede estar vacío");
    }

    @Test
    void shouldRejectContentExceedingMaxSize() {
        String hugeContent = "a".repeat(SourceCode.MAX_SIZE_BYTES + 1);

        assertThatThrownBy(() -> new SourceCode(hugeContent, ProgrammingLanguage.CPP))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("excede el tamaño máximo");
    }

    @Test
    void shouldCalculateSizeInBytes() {
        String content = "abc";
        SourceCode sourceCode = new SourceCode(content, ProgrammingLanguage.PYTHON);

        assertThat(sourceCode.sizeInBytes()).isEqualTo(content.getBytes().length);
    }
}
