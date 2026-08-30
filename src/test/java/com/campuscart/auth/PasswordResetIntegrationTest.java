package com.campuscart.auth;

import com.campuscart.auth.dto.ForgotPasswordRequest;
import com.campuscart.auth.dto.LoginRequest;
import com.campuscart.auth.dto.ResetPasswordRequest;
import com.campuscart.auth.dto.StudentRegistrationRequest;
import com.campuscart.auth.dto.VerifyOtpRequest;
import com.campuscart.auth.dto.VerifyPasswordResetOtpRequest;
import com.campuscart.college.domain.College;
import com.campuscart.college.domain.CollegeEmailDomain;
import com.campuscart.common.exception.ErrorCode;
import com.campuscart.location.domain.City;
import com.campuscart.security.otp.OtpDeliveryGateway;
import com.campuscart.security.otp.OtpDeliveryMessage;
import com.campuscart.security.otp.OtpPurpose;
import com.campuscart.support.AbstractMySqlIntegrationTest;
import com.campuscart.user.domain.User;
import com.campuscart.user.domain.UserType;
import com.campuscart.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Import(PasswordResetIntegrationTest.TestOtpConfiguration.class)
class PasswordResetIntegrationTest extends AbstractMySqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TestOtpDeliveryGateway otpGateway;

    private UUID cityId;
    private UUID collegeId;
    private String domain;

    @BeforeEach
    void seedCollege() {
        otpGateway.clear();
        String suffix = UUID.randomUUID().toString();
        City city = new City("City-" + suffix, "State");
        entityManager.persist(city);
        College college = new College("College-" + suffix, city);
        entityManager.persist(college);
        domain = "col-" + suffix + ".edu.in";
        entityManager.persist(new CollegeEmailDomain(domain, college));
        entityManager.flush();
        cityId = city.getId();
        collegeId = college.getId();
    }

    private User createActiveStudent(String email, String password) {
        College college = entityManager.find(College.class, collegeId);
        User user = User.student(email, "Student User", college, "+9198765" + (int)(Math.random() * 89999 + 10000));
        user.changePasswordHash(passwordEncoder.encode(password));
        user.activateAfterEmailVerification();
        userRepository.saveAndFlush(user);
        return user;
    }

    private User createActiveCommunityUser(String email, String password) {
        City city = entityManager.find(City.class, cityId);
        User user = User.community(email, "Community User", city, "+9198765" + (int)(Math.random() * 89999 + 10000));
        user.changePasswordHash(passwordEncoder.encode(password));
        user.activateAfterEmailVerification();
        userRepository.saveAndFlush(user);
        return user;
    }

    @Test
    @Transactional
    void existingEmailReceivesPasswordResetOtpAndResetsSuccessfully() throws Exception {
        String email = "student1@" + domain;
        createActiveStudent(email, "OldPassword123");

        // 1. Request Password Reset
        JsonNode forgotResp = json(mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new ForgotPasswordRequest(email))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("If an account exists with this email, a verification code has been sent."))
                .andExpect(jsonPath("$.data.challengeId").exists())
                .andReturn().getResponse().getContentAsString());

        String challengeId = forgotResp.get("data").get("challengeId").asText();
        assertThat(otpGateway.messages()).hasSize(1);
        OtpDeliveryMessage msg = otpGateway.last();
        assertThat(msg.purpose()).isEqualTo(OtpPurpose.PASSWORD_RESET);
        assertThat(msg.destination()).isEqualTo(email);
        String code = msg.code();

        // 2. Verify OTP
        JsonNode verifyResp = json(mockMvc.perform(post("/api/v1/auth/forgot-password/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new VerifyPasswordResetOtpRequest(
                                UUID.fromString(challengeId), code))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resetToken").exists())
                .andReturn().getResponse().getContentAsString());

        String resetToken = verifyResp.get("data").get("resetToken").asText();
        assertThat(resetToken).isNotBlank();

        // 3. Reset Password
        mockMvc.perform(post("/api/v1/auth/forgot-password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new ResetPasswordRequest(
                                resetToken, "NewPassword456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 4. Old Password FAILS login
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new LoginRequest(email, "OldPassword123", UserType.STUDENT))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value(ErrorCode.INVALID_CREDENTIALS.name()));

        // 5. New Password SUCCEEDS login
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new LoginRequest(email, "NewPassword456", UserType.STUDENT))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tokens.accessToken").exists());
    }

    @Test
    @Transactional
    void nonExistentEmailReturnsGenericSuccessWithoutSendingEmail() throws Exception {
        String fakeEmail = "doesnotexist@" + domain;

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new ForgotPasswordRequest(fakeEmail))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("If an account exists with this email, a verification code has been sent."))
                .andExpect(jsonPath("$.data.challengeId").exists());

        // No actual email dispatched
        assertThat(otpGateway.messages()).isEmpty();
    }

    @Test
    @Transactional
    void communityUserCanResetPassword() throws Exception {
        String email = "community.user@gmail.com";
        createActiveCommunityUser(email, "OldCommPass123");

        // 1. Request Reset
        JsonNode forgotResp = json(mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new ForgotPasswordRequest(email))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        String challengeId = forgotResp.get("data").get("challengeId").asText();
        String code = otpGateway.last().code();

        // 2. Verify OTP
        JsonNode verifyResp = json(mockMvc.perform(post("/api/v1/auth/forgot-password/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new VerifyPasswordResetOtpRequest(
                                UUID.fromString(challengeId), code))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        String resetToken = verifyResp.get("data").get("resetToken").asText();

        // 3. Reset Password
        mockMvc.perform(post("/api/v1/auth/forgot-password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new ResetPasswordRequest(
                                resetToken, "NewCommPass789"))))
                .andExpect(status().isOk());

        // 4. Log in with new password
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new LoginRequest(email, "NewCommPass789", UserType.COMMUNITY))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tokens.accessToken").exists());
    }

    @Test
    @Transactional
    void wrongOtpIsRejected() throws Exception {
        String email = "wrongotp@" + domain;
        createActiveStudent(email, "Pass123456");

        JsonNode forgotResp = json(mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new ForgotPasswordRequest(email))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        String challengeId = forgotResp.get("data").get("challengeId").asText();

        mockMvc.perform(post("/api/v1/auth/forgot-password/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new VerifyPasswordResetOtpRequest(
                                UUID.fromString(challengeId), "000000"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value(ErrorCode.OTP_INVALID.name()));
    }

    @Test
    @Transactional
    void resetAuthorizationCannotBeReused() throws Exception {
        String email = "singleuse@" + domain;
        createActiveStudent(email, "InitPassword123");

        JsonNode forgotResp = json(mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new ForgotPasswordRequest(email))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        String challengeId = forgotResp.get("data").get("challengeId").asText();
        String code = otpGateway.last().code();

        JsonNode verifyResp = json(mockMvc.perform(post("/api/v1/auth/forgot-password/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new VerifyPasswordResetOtpRequest(
                                UUID.fromString(challengeId), code))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        String resetToken = verifyResp.get("data").get("resetToken").asText();

        // First use -> OK
        mockMvc.perform(post("/api/v1/auth/forgot-password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new ResetPasswordRequest(
                                resetToken, "SecondPassword123"))))
                .andExpect(status().isOk());

        // Reusing the same resetToken -> Unauthorized (INVALID_TOKEN)
        mockMvc.perform(post("/api/v1/auth/forgot-password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new ResetPasswordRequest(
                                resetToken, "ThirdPassword123"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value(ErrorCode.INVALID_TOKEN.name()));
    }

    @Test
    @Transactional
    void activeRefreshTokensAreInvalidatedAfterPasswordReset() throws Exception {
        String email = "revoketokens@" + domain;
        createActiveStudent(email, "CurrentPassword123");

        // Log in to get refresh token
        JsonNode loginResp = json(mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new LoginRequest(email, "CurrentPassword123", UserType.STUDENT))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        String refreshToken = loginResp.get("data").get("tokens").get("refreshToken").asText();

        // Perform password reset
        JsonNode forgotResp = json(mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new ForgotPasswordRequest(email))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        String challengeId = forgotResp.get("data").get("challengeId").asText();
        String code = otpGateway.last().code();

        JsonNode verifyResp = json(mockMvc.perform(post("/api/v1/auth/forgot-password/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new VerifyPasswordResetOtpRequest(
                                UUID.fromString(challengeId), code))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        String resetToken = verifyResp.get("data").get("resetToken").asText();

        mockMvc.perform(post("/api/v1/auth/forgot-password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new ResetPasswordRequest(
                                resetToken, "BrandNewPassword123"))))
                .andExpect(status().isOk());

        // Refresh token rotation must now FAIL (401)
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value(ErrorCode.INVALID_TOKEN.name()));
    }

    @Test
    @Transactional
    void registrationOtpCannotBeUsedAsPasswordResetOtp() throws Exception {
        // Register student -> issues REGISTRATION OTP
        String email = "regotp@" + domain;
        StudentRegistrationRequest request = new StudentRegistrationRequest(
                cityId, collegeId, email, "Reg User", "RegPassword123");

        JsonNode regResp = json(mockMvc.perform(post("/api/v1/auth/register/student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());

        String regChallengeId = regResp.get("data").get("emailOtp").get("challengeId").asText();
        String regCode = otpGateway.last().code();
        assertThat(otpGateway.last().purpose()).isEqualTo(OtpPurpose.REGISTRATION);

        // Attempting to verify this registration challenge on the password reset endpoint -> REJECTED
        mockMvc.perform(post("/api/v1/auth/forgot-password/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new VerifyPasswordResetOtpRequest(
                                UUID.fromString(regChallengeId), regCode))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value(ErrorCode.OTP_INVALID.name()));
    }

    private JsonNode json(String value) throws Exception {
        return objectMapper.readTree(value);
    }

    @TestConfiguration
    static class TestOtpConfiguration {

        @Bean
        @Primary
        TestOtpDeliveryGateway testOtpDeliveryGateway() {
            return new TestOtpDeliveryGateway();
        }
    }

    static class TestOtpDeliveryGateway implements OtpDeliveryGateway {
        private final List<OtpDeliveryMessage> messages = new ArrayList<>();

        @Override
        public void deliver(OtpDeliveryMessage message) {
            messages.add(message);
        }

        OtpDeliveryMessage last() {
            return messages.get(messages.size() - 1);
        }

        List<OtpDeliveryMessage> messages() {
            return List.copyOf(messages);
        }

        void clear() {
            messages.clear();
        }
    }
}
