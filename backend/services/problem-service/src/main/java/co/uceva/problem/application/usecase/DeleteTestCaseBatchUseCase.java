package co.uceva.problem.application.usecase;

import java.util.List;
import java.util.UUID;

public interface DeleteTestCaseBatchUseCase {
    void execute(List<UUID> testCaseIds);
}
