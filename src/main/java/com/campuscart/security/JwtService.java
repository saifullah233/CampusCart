package com.campuscart.security;

import com.campuscart.common.exception.InvalidTokenException;
import com.campuscart.user.domain.Role;
import com.campuscart.user.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Mints and verifies stateless HS256 access tokens.
 *
 * <p>An access token is a signed JWT whose {@code sub} claim is the user's stable id and
 * which additionally carries {@code email} and {@code role} claims. Because the token is
 * signed, the {@link AuthenticatedUser} rebuilt from it is trustworthy without a database
 * lookup — authorization and ownership therefore derive from the signed token, never from
 * request parameters.</p>
 *
 * <p>The signing key is derived from {@link JwtProperties#getSecret()};
 * {@link Keys#hmacShaKeyFor} rejects a secret under 256 bits at construction, so the
 * application fails fast rather than run with a weak key. A {@link Clock} is injected so
 * expiry behaviour is deterministically testable.</p>
 */
@Service
public class JwtService {

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLE = "role";

    private final SecretKey signingKey;
    private final String issuer;
    private final java.time.Duration accessTokenTtl;
    private final Clock clock;
    private final io.jsonwebtoken.Clock jwtClock;

    public JwtService(JwtProperties properties, Clock clock) {
        this.signingKey = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
        this.issuer = properties.getIssuer();
        this.accessTokenTtl = properties.getAccessTokenTtl();
        this.clock = clock;
        this.jwtClock = () -> Date.from(clock.instant());
    }

    /**
     * Issues a signed access token for the given identity.
     *
     * @return the compact JWS string
     */
    public String generateAccessToken(UUID userId, String email, Role role) {
        Instant now = clock.instant();
        Instant expiry = now.plus(accessTokenTtl);
        return Jwts.builder()
                .issuer(issuer)
                .subject(userId.toString())
                .claim(CLAIM_EMAIL, email)
                .claim(CLAIM_ROLE, role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    /** Convenience overload for an already-materialised principal. */
    public String generateAccessToken(AuthenticatedUser user) {
        return generateAccessToken(user.id(), user.email(), user.role());
    }

    /** Issues a token from persisted server-owned identity and authorization fields. */
    public String generateAccessToken(User user) {
        return generateAccessToken(user.getId(), user.getEmail(), user.getRole());
    }

    /**
     * Verifies signature, issuer and expiry, then reconstructs the principal.
     *
     * @throws InvalidTokenException if the token is missing, malformed, tampered,
     *                               expired, or carries unusable claims. The specific
     *                               cause is logged server-side; the thrown message is
     *                               generic so nothing exploitable leaks to the client.
     */
    public AuthenticatedUser parseAccessToken(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidTokenException();
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(issuer)
                    .clock(jwtClock)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String subject = claims.getSubject();
            String email = claims.get(CLAIM_EMAIL, String.class);
            String roleClaim = claims.get(CLAIM_ROLE, String.class);
            if (subject == null || email == null || email.isBlank() || roleClaim == null || roleClaim.isBlank()) {
                throw new InvalidTokenException();
            }
            UUID userId = UUID.fromString(subject);
            Role role = Role.valueOf(roleClaim);
            return new AuthenticatedUser(userId, email, role);
        } catch (JwtException | IllegalArgumentException ex) {
            // Covers signature/format/expiry failures and bad subject/role values.
            // Do not log token material or parser details.
            throw new InvalidTokenException();
        }
    }

    public java.time.Duration getAccessTokenTtl() {
        return accessTokenTtl;
    }
}
