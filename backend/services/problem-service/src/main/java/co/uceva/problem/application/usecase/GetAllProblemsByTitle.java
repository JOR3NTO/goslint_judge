package co.uceva.problem.application.usecase;

import java.util.List;

import co.uceva.problem.domain.model.Problem;

public interface GetAllProblemsByTitle {
    List<Problem> execute(String title);
}
