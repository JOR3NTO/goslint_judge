package co.uceva.problem.infrastructure.web.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import co.uceva.problem.domain.exception.ExistingProblemException;
import co.uceva.problem.domain.exception.ExistingTestCaseException;
import co.uceva.problem.domain.exception.NoProblemsException;
import co.uceva.problem.domain.exception.NoTestCasesException;
import co.uceva.problem.domain.exception.ProblemNotFoundException;
import co.uceva.problem.domain.exception.TestCaseNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ProblemExceptionHandlerTest {

    private final ProblemExceptionHandler handler = new ProblemExceptionHandler();
    private final UUID id = UUID.randomUUID();

    @Test
    void shouldMapMissingResourcesToNotFound() {
        for (RuntimeException ex : new RuntimeException[] {new ProblemNotFoundException(id),
                new TestCaseNotFoundException(id), new NoProblemsException(), new NoTestCasesException()}) {
            var response = handler.handleNotFound(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody().message()).isEqualTo(ex.getMessage());
        }
    }

    @Test
    void shouldMapDuplicatesToConflict() {
        for (RuntimeException ex : new RuntimeException[] {new ExistingProblemException(id),
                new ExistingTestCaseException(id)}) {
            var response = handler.handleConflict(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody().message()).isEqualTo(ex.getMessage());
        }
    }
}
