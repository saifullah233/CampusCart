package com.campuscart.security.refresh;

import com.campuscart.common.exception.InvalidTokenException;
import com.campuscart.common.util.Hashing;
import com.campuscart.common.util.SecureRandomTokens;
import com.campuscart.security.JwtProperties;
import com.campuscart.user.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/**
 * Issues opaque refresh tokens and rotates them atomically.
 *
 * <p>Only a SHA-256 digest is stored. Every successful use revokes the presented
 * record and creates a replacement. Reuse of a revoked token revokes the user's
 * remaining active refresh tokens, which limits the damage of token theft.</p>
 */
@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;
    private final Clock clock;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               JwtProperties jwtProperties,
                               Clock clock) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtProperties = jwtProperties;
        this.clock = clock;
    }

    @Transactional
    public IssuedRefreshToken issueFor(User user) {
        if (user == null || user.getId() == null || !user.getStatus().canAuthenticate()) {
            throw new InvalidTokenException();
        }
        return persistNewToken(user);
    }

    /**
     * Validates and rotates a refresh token under a row lock. The old raw value can
     * never be recovered from the database after this method returns.
     */
    @Transactional
    public IssuedRefreshToken rotate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidTokenException();
        }

        Instant now = clock.instant();
        RefreshToken current = refreshTokenRepository
                .findByTokenHashForUpdate(Hashing.sha256Hex(rawToken))
                .orElseThrow(InvalidTokenException::new);

        if (!current.isActive(now)) {
            revokeAllActiveTokens(current.getUser(), now);
            throw new InvalidTokenException();
        }

        User user = current.getUser();
        if (!user.getStatus().canAuthenticate()) {
            current.revoke(now, null);
            throw new InvalidTokenException();
        }

        IssuedRefreshToken replacement = persistNewToken(user);
        current.revoke(now, replacement.tokenId());
        refreshTokenRepository.save(current);
        return replacement;
    }

    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHashForUpdate(Hashing.sha256Hex(rawToken))
                .ifPresent(token -> token.revoke(clock.instant(), null));
    }

    @Transactional
    public int deleteExpired() {
        return refreshTokenRepository.deleteExpiredBefore(clock.instant());
    }

    private IssuedRefreshToken persistNewToken(User user) {
        String rawToken = SecureRandomTokens.urlSafeToken();
        Instant expiresAt = clock.instant().plus(jwtProperties.getRefreshTokenTtl());
        RefreshToken token = refreshTokenRepository.saveAndFlush(
                new RefreshToken(user, Hashing.sha256Hex(rawToken), expiresAt));
        return new IssuedRefreshToken(rawToken, token.getId(), user.getId(), expiresAt);
    }

    private void revokeAllActiveTokens(User user, Instant now) {
        refreshTokenRepository.findActiveByUserIdForUpdate(user.getId()).forEach(token -> token.revoke(now, null));
    }
}
