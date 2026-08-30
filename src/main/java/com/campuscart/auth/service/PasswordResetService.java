package com.campuscart.auth.service;

import com.campuscart.auth.dto.ForgotPasswordRequest;
import com.campuscart.auth.dto.ForgotPasswordResponse;
import com.campuscart.auth.dto.PasswordResetVerificationResponse;
import com.campuscart.auth.dto.ResetPasswordRequest;
import com.campuscart.auth.dto.VerifyPasswordResetOtpRequest;
import com.campuscart.common.exception.InvalidTokenException;
import com.campuscart.common.util.ContactNormalizer;
import com.campuscart.common.util.Hashing;
import com.campuscart.common.util.SecureRandomTokens;
import com.campuscart.security.login.LoginRateLimitService;
import com.campuscart.security.otp.OtpChallenge;
import com.campuscart.security.otp.OtpChannel;
import com.campuscart.security.otp.OtpProperties;
import com.campuscart.security.otp.OtpPurpose;
import com.campuscart.security.otp.PasswordResetAuthorization;
import com.campuscart.security.otp.PasswordResetAuthorizationRepository;
import com.campuscart.security.refresh.RefreshTokenService;
import com.campuscart.user.domain.AccountStatus;
import com.campuscart.user.domain.User;
import com.campuscart.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Orchestrates secure password reset: generic account enumeration mitigation,
 * OTP verification with dedicated PASSWORD_RESET purpose, single-use reset authorizations,
 * and session/refresh-token invalidation upon password update.
 */
@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final Duration RESET_TOKEN_TTL = Duration.ofMinutes(15);
    private static final String DUMMY_HASH = "$2a$12$e8762514371234567890123456789012345678901234567890123";

    private final UserRepository userRepository;
    private final OtpService otpService;
    private final OtpProperties otpProperties;
    private final PasswordResetAuthorizationRepository resetAuthorizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final LoginRateLimitService loginRateLimitService;
    private final Clock clock;

    public PasswordResetService(UserRepository userRepository,
                                OtpService otpService,
                                OtpProperties otpProperties,
                                PasswordResetAuthorizationRepository resetAuthorizationRepository,
                                PasswordEncoder passwordEncoder,
                                RefreshTokenService refreshTokenService,
                                LoginRateLimitService loginRateLimitService,
                                Clock clock) {
        this.userRepository = userRepository;
        this.otpService = otpService;
        this.otpProperties = otpProperties;
        this.resetAuthorizationRepository = resetAuthorizationRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
        this.loginRateLimitService = loginRateLimitService;
        this.clock = clock;
    }

    @Transactional
    public ForgotPasswordResponse requestPasswordReset(ForgotPasswordRequest request) {
        String email = ContactNormalizer.email(request.email());
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isPresent() && userOpt.get().getStatus() != AccountStatus.SUSPENDED) {
            User user = userOpt.get();
            OtpChallengeResponse challengeResponse = otpService.issueForPurpose(
                    user, OtpChannel.EMAIL, OtpPurpose.PASSWORD_RESET, email);
            return new ForgotPasswordResponse(
                    challengeResponse.challengeId(),
                    challengeResponse.destination(),
                    challengeResponse.expiresAt(),
                    challengeResponse.nextResendAt()
            );
        }

        // Timing mitigation for non-existent or suspended users
        try {
            passwordEncoder.matches("timing-mitigation-pass", DUMMY_HASH);
        } catch (Exception ignored) {
        }

        Instant now = clock.instant();
        return new ForgotPasswordResponse(
                UUID.randomUUID(),
                OtpService.maskDestination(email, OtpChannel.EMAIL),
                now.plus(otpProperties.getTtl()),
                now.plus(otpProperties.getResendCooldown())
        );
    }

    @Transactional
    public PasswordResetVerificationResponse verifyOtp(VerifyPasswordResetOtpRequest request) {
        OtpChallenge challenge = otpService.verifyChallenge(
                request.challengeId(), request.code(), OtpPurpose.PASSWORD_RESET);

        String rawResetToken = SecureRandomTokens.urlSafeToken(32);
        String tokenHash = Hashing.sha256Hex(rawResetToken);
        Instant expiresAt = clock.instant().plus(RESET_TOKEN_TTL);

        PasswordResetAuthorization auth = new PasswordResetAuthorization(
                challenge.getUser(), challenge, tokenHash, expiresAt);
        resetAuthorizationRepository.saveAndFlush(auth);

        return new PasswordResetVerificationResponse(rawResetToken, expiresAt);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (request.resetToken() == null || request.resetToken().isBlank()) {
            throw new InvalidTokenException();
        }

        Instant now = clock.instant();
        String tokenHash = Hashing.sha256Hex(request.resetToken());
        PasswordResetAuthorization auth = resetAuthorizationRepository
                .findByTokenHashForUpdate(tokenHash)
                .orElseThrow(InvalidTokenException::new);

        if (!auth.isValid(now)) {
            throw new InvalidTokenException();
        }

        User user = auth.getUser();
        user.changePasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        auth.markUsed(now);
        resetAuthorizationRepository.save(auth);

        refreshTokenService.revokeAllForUser(user.getId());
        loginRateLimitService.recordSuccess(user.getEmail());
    }

    @Transactional
    public ForgotPasswordResponse resendOtp(UUID challengeId) {
        OtpChallengeResponse response = otpService.resend(challengeId);
        return new ForgotPasswordResponse(
                response.challengeId(),
                response.destination(),
                response.expiresAt(),
                response.nextResendAt()
        );
    }
}
