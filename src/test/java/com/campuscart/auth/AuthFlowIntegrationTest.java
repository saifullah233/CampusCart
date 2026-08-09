package com.campuscart.auth;

import com.campuscart.auth.dto.CommunityRegistrationRequest;
import com.campuscart.auth.dto.LoginRequest;
import com.campuscart.auth.dto.StudentRegistrationRequest;
import com.campuscart.auth.dto.VerifyOtpRequest;
import com.campuscart.college.domain.College;
import com.campuscart.college.domain.CollegeEmailDomain;
import com.campuscart.common.exception.ErrorCode;
import com.campuscart.location.domain.City;
import com.campuscart.security.otp.OtpDeliveryGateway;
import com.campuscart.security.otp.OtpDeliveryMessage;
import com.campuscart.support.AbstractMySqlIntegrationTest;
import com.campuscart.user.domain.User;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Import(AuthFlowIntegrationTest.TestOtpConfiguration.class)
class AuthFlowIntegrationTest extends AbstractMySqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestOtpDeliveryGateway otpGateway;

    private UUID cityId;
    private UUID collegeId;

    @BeforeEach
    void seedCollege() {
        otpGateway.clear();
        String suffix = UUID.randomUUID().toString();
        City city = new City("Mumbai-" + suffix, "Maharashtra");
        entityManager.persist(city);
        College college = new College("IIT Bombay-" + suffix, city);
        entityManager.persist(college);
        entityManager.persist(new CollegeEmailDomain("iitb-" + suffix + ".ac.in", college));
        entityManager.flush();
        cityId = city.getId();
        collegeId = college.getId();
    }

    @Test
    @Transactional
    void studentRegistrationValidatesDomainVerifiesAndLogsIn() throws Exception {
        // Use the configured domain from the persisted fixture rather than trusting a client college id.
        String domain = entityManager.createQuery(
                        "select domain.domain from CollegeEmailDomain domain where domain.college.id = :collegeId",
                        String.class)
                .setParameter("collegeId", collegeId)
                .getSingleResult();
        String email = "student@" + domain;
        StudentRegistrationRequest request = new StudentRegistrationRequest(
                cityId, collegeId, email, "Student User", "correct horse battery staple");

        JsonNode registration = json(mockMvc.perform(post("/api/v1/auth/register/student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING_VERIFICATION"))
                .andReturn().getResponse().getContentAsString());

        assertThat(otpGateway.last().channel().name()).isEqualTo("EMAIL");
        String accessToken = verify(otpGateway.last().code(), registration.get("data").get("otp").get("challengeId").asText());
        assertThat(accessToken).isNotBlank();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new LoginRequest(
                                email, "correct horse battery staple"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountType").value("STUDENT"))
                .andExpect(jsonPath("$.data.emailVerified").value(true))
                .andExpect(jsonPath("$.data.collegeId").value(collegeId.toString()));
    }

    @Test
    @Transactional
    void communityRegistrationUsesPhoneOtpAndHasNoCollegeAssociation() throws Exception {
        String email = "community-" + UUID.randomUUID() + "@example.com";
        CommunityRegistrationRequest request = new CommunityRegistrationRequest(
                email, "Community User", cityId, "+919876543210", "community password");

        JsonNode registration = json(mockMvc.perform(post("/api/v1/auth/register/community")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());

        assertThat(otpGateway.last().channel().name()).isEqualTo("PHONE");
        String accessToken = verify(otpGateway.last().code(), registration.get("data").get("otp").get("challengeId").asText());

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountType").value("COMMUNITY"))
                .andExpect(jsonPath("$.data.phoneVerified").value(true))
                .andExpect(jsonPath("$.data.collegeId").doesNotExist());
    }

    @Test
    @Transactional
    void invalidCollegeEmailIsRejected() throws Exception {
        StudentRegistrationRequest request = new StudentRegistrationRequest(
                cityId, collegeId, "student@not-configured.example", "Student User", "password123");

        mockMvc.perform(post("/api/v1/auth/register/student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value(ErrorCode.BUSINESS_RULE_VIOLATION.name()));
        assertThat(otpGateway.messages()).isEmpty();
    }

    @Test
    @Transactional
    void invalidOtpIsRejectedAndValidOtpStillWorksBeforeLimit() throws Exception {
        JsonNode registration = registerStudent();
        String challengeId = registration.get("data").get("otp").get("challengeId").asText();
        String wrongCode = otpGateway.last().code().equals("000000") ? "111111" : "000000";

        mockMvc.perform(post("/api/v1/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new VerifyOtpRequest(
                                UUID.fromString(challengeId), wrongCode))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("OTP_INVALID"));

        verify(otpGateway.last().code(), challengeId);
    }

    @Test
    @Transactional
    void otpBruteForceIsStoppedAfterConfiguredAttempts() throws Exception {
        JsonNode registration = registerStudent();
        UUID challengeId = UUID.fromString(registration.get("data").get("otp").get("challengeId").asText());
        String wrongCode = otpGateway.last().code().equals("000000") ? "111111" : "000000";

        for (int attempt = 1; attempt <= 4; attempt++) {
            mockMvc.perform(post("/api/v1/otp/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsBytes(new VerifyOtpRequest(challengeId, wrongCode))))
                    .andExpect(status().isBadRequest());
        }
        mockMvc.perform(post("/api/v1/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new VerifyOtpRequest(challengeId, wrongCode))))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("OTP_ATTEMPTS_EXCEEDED"));
    }

    @Test
    @Transactional
    void otpResendCooldownIsEnforced() throws Exception {
        JsonNode registration = registerStudent();
        UUID challengeId = UUID.fromString(registration.get("data").get("otp").get("challengeId").asText());

        mockMvc.perform(post("/api/v1/otp/resend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(
                                new com.campuscart.auth.dto.ResendOtpRequest(challengeId))))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("OTP_COOLDOWN"));
    }

    @Test
    @Transactional
    void refreshRotationAndLogoutInvalidatePresentedSession() throws Exception {
        JsonNode registration = registerStudent();
        JsonNode verified = verifyResponse(otpGateway.last().code(),
                registration.get("data").get("otp").get("challengeId").asText());
        String firstRefresh = verified.get("data").get("tokens").get("refreshToken").asText();

        JsonNode rotated = json(mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new com.campuscart.auth.dto.RefreshTokenRequest(firstRefresh))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        String secondRefresh = rotated.get("data").get("refreshToken").asText();
        assertThat(secondRefresh).isNotEqualTo(firstRefresh);

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new com.campuscart.auth.dto.RefreshTokenRequest(secondRefresh))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new com.campuscart.auth.dto.RefreshTokenRequest(secondRefresh))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_TOKEN"));
    }

    @Test
    @Transactional
    void suspendedAccountCannotLoginOrUseProfile() throws Exception {
        JsonNode registration = registerStudent();
        String email = entityManager.createQuery("select user.email from User user where user.id = :id", String.class)
                .setParameter("id", UUID.fromString(registration.get("data").get("userId").asText()))
                .getSingleResult();
        String accessToken = verify(otpGateway.last().code(),
                registration.get("data").get("otp").get("challengeId").asText());
        User user = userRepository.findById(UUID.fromString(registration.get("data").get("userId").asText())).orElseThrow();
        user.suspend();
        userRepository.saveAndFlush(user);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new LoginRequest(email, "password123"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCOUNT_NOT_ACTIVE"));
        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCOUNT_NOT_ACTIVE"));
    }

    private JsonNode registerStudent() throws Exception {
        String domain = entityManager.createQuery(
                        "select domain.domain from CollegeEmailDomain domain where domain.college.id = :collegeId",
                        String.class)
                .setParameter("collegeId", collegeId)
                .getSingleResult();
        StudentRegistrationRequest request = new StudentRegistrationRequest(
                cityId, collegeId, "student-" + UUID.randomUUID() + "@" + domain,
                "Student User", "password123");
        return json(mockMvc.perform(post("/api/v1/auth/register/student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
    }

    private String verify(String code, String challengeId) throws Exception {
        return verifyResponse(code, challengeId).get("data").get("tokens").get("accessToken").asText();
    }

    private JsonNode verifyResponse(String code, String challengeId) throws Exception {
        return json(mockMvc.perform(post("/api/v1/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new VerifyOtpRequest(
                                UUID.fromString(challengeId), code))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
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
