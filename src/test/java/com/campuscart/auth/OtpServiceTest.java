package com.campuscart.auth;

import com.campuscart.auth.service.OtpService;
import com.campuscart.common.exception.ApiException;
import com.campuscart.security.otp.OtpChallenge;
import com.campuscart.security.otp.OtpChallengeRepository;
import com.campuscart.security.otp.OtpChannel;
import com.campuscart.security.otp.OtpDeliveryGateway;
import com.campuscart.security.otp.OtpInvalidException;
import com.campuscart.security.otp.OtpProperties;
import com.campuscart.security.otp.OtpPurpose;
import com.campuscart.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock
    private OtpChallengeRepository challengeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private OtpDeliveryGateway deliveryGateway;

    @Test
    void expiredOtpCannotBeVerified() {
        Instant now = Instant.parse("2026-08-09T00:00:00Z");
        OtpChallenge challenge = new OtpChallenge(
                null,
                OtpChannel.EMAIL,
                OtpPurpose.REGISTRATION,
                "destination-hash",
                "code-hash",
                now.minusSeconds(1),
                now.minusSeconds(1));
        UUID challengeId = UUID.randomUUID();
        when(challengeRepository.findByIdForUpdate(challengeId)).thenReturn(Optional.of(challenge));

        OtpService service = new OtpService(
                challengeRepository,
                userRepository,
                passwordEncoder,
                deliveryGateway,
                properties(),
                Clock.fixed(now, ZoneOffset.UTC));

        assertThatThrownBy(() -> service.verify(challengeId, "123456"))
                .isInstanceOf(OtpInvalidException.class)
                .isInstanceOf(ApiException.class);
    }

    private OtpProperties properties() {
        OtpProperties properties = new OtpProperties();
        properties.setMaxAttempts(5);
        return properties;
    }
}
