package com.campuscart.security.refresh;

import java.time.Instant;
import java.util.UUID;

/** Raw refresh token returned only to the caller that will send it to the client. */
public record IssuedRefreshToken(String rawToken, UUID tokenId, UUID userId, Instant expiresAt) {
}
