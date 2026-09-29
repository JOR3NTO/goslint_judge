package co.uceva.judge.infrastructure.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import co.uceva.shared.infrastructure.security.AuthenticatedUser;
import co.uceva.shared.infrastructure.security.InvalidTokenException;
import co.uceva.shared.infrastructure.security.JwtTokenValidator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServiceTokenProviderTest {

    private static final String SECRET = "una-clave-compartida-de-al-menos-32-bytes!!";
    private static final String ISSUER = "goslint-judge";

    private final UUID serviceId = UUID.randomUUID();

    private static Clock at(Instant instant) {
        return Clock.fixed(instant, ZoneOffset.UTC);
    }

    @Test
    void shouldIssueTokenAcceptedBySharedValidatorWithServiceRole() {
        ServiceTokenProvider provider = new ServiceTokenProvider(SECRET, ISSUER, serviceId,
                Duration.ofMinutes(5), Clock.systemUTC());

        AuthenticatedUser user = new JwtTokenValidator(SECRET, ISSUER).validate(provider.token());

        assertThat(user.userId()).isEqualTo(serviceId);
        assertThat(user.role()).isEqualTo("SERVICE");
    }

    @Test
    void shouldReuseTokenWhileItIsStillValid() {
        ServiceTokenProvider provider = new ServiceTokenProvider(SECRET, ISSUER, serviceId,
                Duration.ofMinutes(5), at(Instant.parse("2026-09-20T12:00:00Z")));

        assertThat(provider.token()).isSameAs(provider.token());
    }

    @Test
    void shouldIssueNewTokenNearExpiration() {
        Instant start = Instant.parse("2026-09-20T12:00:00Z");
        MutableClock clock = new MutableClock(start);
        ServiceTokenProvider provider = new ServiceTokenProvider(SECRET, ISSUER, serviceId, Duration.ofMinutes(5), clock);

        String first = provider.token();
        clock.now = start.plus(Duration.ofMinutes(5)).minusSeconds(10);

        assertThat(provider.token()).isNotEqualTo(first);
    }

    @Test
    void shouldBeRejectedByValidatorUsingAnotherSecret() {
        String token = new ServiceTokenProvider(SECRET, ISSUER, serviceId, Duration.ofMinutes(5), Clock.systemUTC()).token();

        assertThatThrownBy(() -> new JwtTokenValidator("otra-clave-distinta-de-al-menos-32-bytes!", ISSUER).validate(token))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void shouldRejectShortSecret() {
        assertThatThrownBy(() -> new ServiceTokenProvider("corta", ISSUER, serviceId, Duration.ofMinutes(5), Clock.systemUTC()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
