package com.campuscart.security.otp;

import java.time.Instant;

/** In-memory delivery message; the raw code is never persisted or logged. */
public record OtpDeliveryMessage(OtpChannel channel, String destination, String code, Instant expiresAt) {
}
