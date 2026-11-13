package co.uceva.contests_platform.contest.infrastructure.scoreboard;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import co.uceva.contests_platform.contest.application.event.ScoreboardFrozenEvent;
import co.uceva.contests_platform.contest.application.event.ScoreboardUnfrozenEvent;
import co.uceva.contests_platform.contest.application.event.TeamScoreChangedEvent;
import co.uceva.contests_platform.contest.application.service.ScoreboardCalculator;
import co.uceva.contests_platform.contest.application.usecase.UpdateTeamScoreUseCase;
import co.uceva.contests_platform.submission.application.event.SubmissionStatusChangedEvent;
import co.uceva.shared.domain.VerdictStatus;

/**
 * Observa la calificación de los envíos para mantener el scoreboard.
 * <p>
 * El puntaje del equipo se recalcula dentro de la misma transacción que guarda el veredicto,
 * así ambos se confirman juntos. La caché y los clientes SSE se actualizan solo después del commit.
 * </p>
 */
@Component
public class ScoreboardListener {

    private final UpdateTeamScoreUseCase updateTeamScoreUseCase;
    private final ScoreboardCache scoreboardCache;
    private final ScoreboardCalculator scoreboardCalculator;

    public ScoreboardListener(UpdateTeamScoreUseCase updateTeamScoreUseCase, ScoreboardCache scoreboardCache,
            ScoreboardCalculator scoreboardCalculator) {
        this.updateTeamScoreUseCase = updateTeamScoreUseCase;
        this.scoreboardCache = scoreboardCache;
        this.scoreboardCalculator = scoreboardCalculator;
    }

    @EventListener
    public void onSubmissionStatusChanged(SubmissionStatusChangedEvent event) {
        if (event.submission().getVerdict() == VerdictStatus.PENDING) {
            return;
        }
        updateTeamScoreUseCase.execute(event.submission().getTeamId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTeamScoreChanged(TeamScoreChangedEvent event) {
        scoreboardCache.update(event.row());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onScoreboardFrozen(ScoreboardFrozenEvent event) {
        scoreboardCache.freeze(event.contest().getId(),
                scoreboardCalculator.build(event.contest(), event.contest().getFreezeTime()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onScoreboardUnfrozen(ScoreboardUnfrozenEvent event) {
        scoreboardCache.unfreeze(event.contest().getId(), scoreboardCalculator.build(event.contest(), null));
    }
}
