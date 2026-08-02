package co.uceva.problem.application.usecase;

import java.util.List;
import java.util.UUID;

public interface ReorderTestCaseUseCase {
    void execute(UUID problemId, List<UUID> orderedTestCaseIds);
}
