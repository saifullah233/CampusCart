package com.campuscart.security;

import com.campuscart.common.exception.InvalidTokenException;
import com.campuscart.common.util.SecureRandomTokens;
import com.campuscart.user.domain.Role;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String TEST_SECRET = SecureRandomTokens.urlSafeToken(32);
    private static final Instant START = Instant.parse("2026-08-09T00:00:00Z");

    @Test
    void createsAndParsesSignedAccessToken() {
        MutableClock clock = new MutableClock(START);
        JwtProperties properties = properties(Duration.ofMinutes(15));
        JwtService service = new JwtService(properties, clock);
        UUID userId = UUID.randomUUID();

        String token = service.generateAccessToken(userId, "student@example.edu", Role.STUDENT);

        assertThat(service.parseAccessToken(token))
                .satisfies(user -> {
                    assertThat(user.id()).isEqualTo(userId);
                    assertThat(user.email()).isEqualTo("student@example.edu");
                    assertThat(user.role()).isEqualTo(Role.STUDENT);
                });
    }

    @Test
    void rejectsExpiredAccessTokenWithGenericException() {
        MutableClock clock = new MutableClock(START);
        JwtService service = new JwtService(properties(Duration.ofMinutes(15)), clock);
        String token = service.generateAccessToken(UUID.randomUUID(), "student@example.edu", Role.STUDENT);

        clock.advance(Duration.ofMinutes(16));

        assertThatThrownBy(() -> service.parseAccessToken(token))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("The token is invalid or has expired.");
    }

    @Test
    void rejectsTamperedAccessToken() {
        JwtService service = new JwtService(properties(Duration.ofMinutes(15)), Clock.fixed(START, ZoneId.of("UTC")));
        String token = service.generateAccessToken(UUID.randomUUID(), "student@example.edu", Role.STUDENT);

        String tampered = token.substring(0, token.length() - 1) + (token.endsWith("a") ? "b" : "a");

        assertThatThrownBy(() -> service.parseAccessToken(tampered))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("The token is invalid or has expired.");
    }

    private JwtProperties properties(Duration accessTokenTtl) {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(TEST_SECRET);
        properties.setAccessTokenTtl(accessTokenTtl);
        return properties;
    }

    private static final class MutableClock extends Clock {
        private Instant current;

        private MutableClock(Instant current) {
            this.current = current;
        }

        private void advance(Duration duration) {
            current = current.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return current;
        }
    }
}
