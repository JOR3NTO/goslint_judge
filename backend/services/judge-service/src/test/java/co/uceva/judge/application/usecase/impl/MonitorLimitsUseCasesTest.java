package co.uceva.judge.application.usecase.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.uceva.judge.domain.model.MonitorLimits;
import co.uceva.judge.domain.repository.MonitorLimitsRepository;
import co.uceva.judge.domain.valueobject.AbsoluteTimeLimit;
import co.uceva.judge.domain.valueobject.ErrorSizeLimit;
import co.uceva.judge.domain.valueobject.HardTimePercent;
import co.uceva.judge.domain.valueobject.OutputSizeLimit;
import co.uceva.judge.domain.valueobject.WatchIntervalMillis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonitorLimitsUseCasesTest {

    @Mock private MonitorLimitsRepository repository;

    @Test
    void shouldReturnCurrentLimits() {
        MonitorLimits current = MonitorLimits.ofDefault();
        when(repository.find()).thenReturn(current);

        assertThat(new GetMonitorLimitsUseCaseImpl(repository).execute()).isSameAs(current);
    }

    @Test
    void shouldSaveAndReturnNewLimits() {
        MonitorLimits updated = new MonitorLimits(new OutputSizeLimit(2_048), new ErrorSizeLimit(1_024), new HardTimePercent(0.5f),
                new WatchIntervalMillis(100), new AbsoluteTimeLimit(5_000));

        MonitorLimits result = new UpdateMonitorLimitsUseCaseImpl(repository).execute(updated);

        verify(repository).save(updated);
        assertThat(result).isSameAs(updated);
    }

    @Test
    void shouldRejectMissingLimit() {
        assertThatThrownBy(() -> new MonitorLimits(null, ErrorSizeLimit.ofDefault(), HardTimePercent.ofDefault(), WatchIntervalMillis.ofDefault(),
                AbsoluteTimeLimit.ofDefault())).isInstanceOf(IllegalArgumentException.class);
    }
}
