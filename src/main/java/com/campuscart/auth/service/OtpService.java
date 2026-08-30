package com.campuscart.auth.service;

import com.campuscart.common.exception.BusinessRuleException;
import com.campuscart.common.exception.InvalidTokenException;
import com.campuscart.common.util.ContactNormalizer;
import com.campuscart.common.util.Hashing;
import com.campuscart.common.util.SecureRandomTokens;
import com.campuscart.security.otp.OtpAlreadyVerifiedException;
import com.campuscart.security.otp.OtpAttemptsExceededException;
import com.campuscart.security.otp.OtpChallenge;
import com.campuscart.security.otp.OtpChallengeRepository;
import com.campuscart.security.otp.OtpChannel;
import com.campuscart.security.otp.OtpCooldownException;
import com.campuscart.security.otp.OtpDeliveryGateway;
import com.campuscart.security.otp.OtpDeliveryMessage;
import com.campuscart.security.otp.OtpInvalidException;
import com.campuscart.security.otp.OtpProperties;
import com.campuscart.security.otp.OtpPurpose;
import com.campuscart.security.otp.OtpRateLimitedException;
import com.campuscart.user.domain.User;
import com.campuscart.user.domain.UserType;
import com.campuscart.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Creates, delivers, rate-limits, and verifies one-time registration challenges. */
@Service
public class OtpService {

    private final OtpChallengeRepository challengeRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpDeliveryGateway deliveryGateway;
    private final OtpProperties properties;
    private final Clock clock;

    public OtpService(OtpChallengeRepository challengeRepository,
                      UserRepository userRepository,
                      PasswordEncoder passwordEncoder,
                      OtpDeliveryGateway deliveryGateway,
                      OtpProperties properties,
                      Clock clock) {
        this.challengeRepository = challengeRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.deliveryGateway = deliveryGateway;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public void ensureCanIssue(String destination) {
        String hash = Hashing.sha256Hex(destination);
        Instant since = clock.instant().minus(properties.getRateWindow());
        if (challengeRepository.countRecentByDestinationHash(hash, since)
                >= properties.getMaxSendsPerWindow()) {
            throw new OtpRateLimitedException();
        }
    }

    @Transactional
    public OtpChallengeResponse issue(User user, OtpChannel channel, String destination) {
        return issueForPurpose(user, channel, OtpPurpose.REGISTRATION, destination);
    }

    @Transactional
    public OtpChallengeResponse issueForPurpose(User user, OtpChannel channel, OtpPurpose purpose, String destination) {
        ensureCanIssue(destination);
        Instant now = clock.instant();
        Optional<OtpChallenge> activeOpt = challengeRepository.findActiveUnverifiedChallengeByPurpose(user.getId(), channel, purpose, now);
        if (activeOpt.isPresent()) {
            OtpChallenge active = activeOpt.get();
            active.markSuperseded(now);
            challengeRepository.saveAndFlush(active);
        }

        String code = SecureRandomTokens.numericCode(properties.getCodeLength());
        Instant expiresAt = now.plus(properties.getTtl());
        OtpChallenge challenge = challengeRepository.saveAndFlush(new OtpChallenge(
                user,
                channel,
                purpose,
                Hashing.sha256Hex(destination),
                passwordEncoder.encode(code),
                expiresAt,
                now.plus(properties.getResendCooldown())));
        deliveryGateway.deliver(new OtpDeliveryMessage(channel, destination, code, expiresAt, purpose));
        return OtpChallengeResponse.from(challenge, destination);
    }

    /**
     * Issues a fresh challenge or safely reuses an active challenge during cooldown to prevent spam.
     */
    @Transactional
    public OtpChallengeResponse issueOrRenew(User user, OtpChannel channel, String destination) {
        Instant now = clock.instant();
        Optional<OtpChallenge> activeOpt = challengeRepository.findActiveUnverifiedChallenge(user.getId(), channel, now);
        if (activeOpt.isPresent()) {
            OtpChallenge active = activeOpt.get();
            if (active.getNextResendAt().isAfter(now) && active.getAttemptCount() < properties.getMaxAttempts()) {
                return OtpChallengeResponse.from(active, destination);
            }
            active.markSuperseded(now);
            challengeRepository.saveAndFlush(active);
        }
        return issue(user, channel, destination);
    }

    @Transactional(noRollbackFor = {
            OtpInvalidException.class,
            OtpAttemptsExceededException.class,
            OtpCooldownException.class,
            OtpRateLimitedException.class,
            OtpAlreadyVerifiedException.class
    })
    public OtpChallenge verifyChallenge(UUID challengeId, String rawCode, OtpPurpose expectedPurpose) {
        if (challengeId == null || rawCode == null || !rawCode.matches("\\d{4,8}")) {
            throw new OtpInvalidException();
        }
        OtpChallenge challenge = challengeRepository.findByIdForUpdate(challengeId)
                .orElseThrow(OtpInvalidException::new);
        if (challenge.isVerified()) {
            throw new OtpAlreadyVerifiedException();
        }
        if (expectedPurpose != null && challenge.getPurpose() != expectedPurpose) {
            throw new OtpInvalidException();
        }
        Instant now = clock.instant();
        if (challenge.getAttemptCount() >= properties.getMaxAttempts()) {
            throw new OtpAttemptsExceededException();
        }
        if (!challenge.isUsable(now, properties.getMaxAttempts())) {
            throw new OtpInvalidException();
        }
        if (!passwordEncoder.matches(rawCode, challenge.getCodeHash())) {
            challenge.recordFailedAttempt();
            challengeRepository.saveAndFlush(challenge);
            if (challenge.getAttemptCount() >= properties.getMaxAttempts()) {
                throw new OtpAttemptsExceededException();
            }
            throw new OtpInvalidException();
        }

        challenge.markVerified(now);
        challengeRepository.saveAndFlush(challenge);
        return challenge;
    }

    @Transactional(noRollbackFor = {
            OtpInvalidException.class,
            OtpAttemptsExceededException.class,
            OtpCooldownException.class,
            OtpRateLimitedException.class,
            OtpAlreadyVerifiedException.class
    })
    public User verify(UUID challengeId, String rawCode) {
        OtpChallenge challenge = verifyChallenge(challengeId, rawCode, OtpPurpose.REGISTRATION);
        User user = challenge.getUser();
        if (challenge.getChannel() == OtpChannel.EMAIL) {
            user.markEmailVerified();
        } else if (challenge.getChannel() == OtpChannel.PHONE) {
            user.markPhoneVerified();
        } else {
            throw new BusinessRuleException("This verification channel is not valid for the account.");
        }
        userRepository.save(user);
        return user;
    }

    @Transactional(noRollbackFor = {OtpCooldownException.class, OtpRateLimitedException.class})
    public OtpChallengeResponse resend(UUID challengeId) {
        OtpChallenge current = challengeRepository.findByIdForUpdate(challengeId)
                .orElseThrow(OtpInvalidException::new);
        if (current.isVerified()) {
            throw new OtpAlreadyVerifiedException();
        }
        Instant now = clock.instant();
        if (current.getNextResendAt().isAfter(now)) {
            throw new OtpCooldownException();
        }
        User user = current.getUser();
        String destination = destination(user, current.getChannel());
        ensureCanIssue(destination);
        current.markSuperseded(now);
        String code = SecureRandomTokens.numericCode(properties.getCodeLength());
        Instant expiresAt = now.plus(properties.getTtl());
        OtpChallenge replacement = challengeRepository.saveAndFlush(new OtpChallenge(
                user,
                current.getChannel(),
                current.getPurpose(),
                current.getDestinationHash(),
                passwordEncoder.encode(code),
                expiresAt,
                now.plus(properties.getResendCooldown())));
        deliveryGateway.deliver(new OtpDeliveryMessage(current.getChannel(), destination, code, expiresAt, current.getPurpose()));
        return OtpChallengeResponse.from(replacement, destination);
    }

    private String destination(User user, OtpChannel channel) {
        String destination = channel == OtpChannel.EMAIL ? user.getEmail() : user.getPhoneNumber();
        if (destination == null || destination.isBlank()) {
            throw new InvalidTokenException();
        }
        return channel == OtpChannel.EMAIL ? ContactNormalizer.email(destination)
                : ContactNormalizer.phone(destination);
    }

    public static String maskDestination(String destination, OtpChannel channel) {
        if (channel == OtpChannel.EMAIL) {
            String normalized = ContactNormalizer.email(destination);
            int at = normalized.indexOf('@');
            return normalized.substring(0, 1) + "***" + normalized.substring(at);
        }
        String normalized = ContactNormalizer.phone(destination);
        return "***" + normalized.substring(Math.max(0, normalized.length() - 4));
    }
}
