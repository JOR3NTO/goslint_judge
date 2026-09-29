package co.uceva.judge.infrastructure.persistence;

import org.junit.jupiter.api.Test;

import co.uceva.judge.domain.model.MonitorLimits;
import co.uceva.judge.domain.valueobject.AbsoluteTimeLimit;
import co.uceva.judge.domain.valueobject.ErrorSizeLimit;
import co.uceva.judge.domain.valueobject.HardTimePercent;
import co.uceva.judge.domain.valueobject.OutputSizeLimit;
import co.uceva.judge.domain.valueobject.WatchIntervalMillis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryMonitorLimitsRepositoryTest {

    @Test
    void shouldStartWithConfiguredValues() {
        InMemoryMonitorLimitsRepository repository = new InMemoryMonitorLimitsRepository(4_096, 2_048, 0.5f, 200, 8_000);

        MonitorLimits limits = repository.find();

        assertThat(limits.outputSize().bytes()).isEqualTo(4_096);
        assertThat(limits.errorSize().bytes()).isEqualTo(2_048);
        assertThat(limits.hardTimePercent().percentage()).isEqualTo(0.5f);
        assertThat(limits.watchInterval().milliseconds()).isEqualTo(200);
        assertThat(limits.absoluteTimeLimit().milliseconds()).isEqualTo(8_000);
    }

    @Test
    void shouldReplaceLimitsOnSave() {
        InMemoryMonitorLimitsRepository repository = new InMemoryMonitorLimitsRepository(4_096, 2_048, 0.5f, 200, 8_000);
        MonitorLimits updated = new MonitorLimits(new OutputSizeLimit(2_048), new ErrorSizeLimit(1_024), new HardTimePercent(0.1f),
                new WatchIntervalMillis(100), new AbsoluteTimeLimit(2_000));

        repository.save(updated);

        assertThat(repository.find()).isSameAs(updated);
    }

    @Test
    void shouldFailToStartWithInvalidErrorSize() {
        assertThatThrownBy(() -> new InMemoryMonitorLimitsRepository(4_096, 1, 0.5f, 200, 8_000))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldFailToStartWithInvalidConfiguration() {
        assertThatThrownBy(() -> new InMemoryMonitorLimitsRepository(1, 2_048, 0.5f, 200, 8_000))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
