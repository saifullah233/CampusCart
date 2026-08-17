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
                cityId, collegeId, email, "Student User", "+919876543210", "correct horse battery staple");

        JsonNode registration = json(mockMvc.perform(post("/api/v1/auth/register/student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING_VERIFICATION"))
                .andExpect(jsonPath("$.data.emailOtp").exists())
                .andExpect(jsonPath("$.data.phoneOtp").doesNotExist())
                .andReturn().getResponse().getContentAsString());

        // Verify phone number is saved in DB
        User userInDb = userRepository.findByEmail(email).orElseThrow();
        assertThat(userInDb.getPhoneNumber()).isEqualTo("+919876543210");
        assertThat(userInDb.isEmailVerified()).isFalse();

        String emailChallengeId = registration.get("data").get("emailOtp").get("challengeId").asText();

        // Verify that ONLY an EMAIL OTP message was dispatched (no PHONE OTP)
        assertThat(otpGateway.messages()).hasSize(1);
        OtpDeliveryMessage emailMsg = otpGateway.messages().get(0);
        assertThat(emailMsg.channel().name()).isEqualTo("EMAIL");

        // Verify Email OTP alone -> user activates immediately and tokens are issued!
        JsonNode emailVerifyRes = verifyResponse(emailMsg.code(), emailChallengeId);
        assertThat(emailVerifyRes.get("data").get("tokens").get("accessToken").asText()).isNotBlank();
        assertThat(emailVerifyRes.get("data").get("user").get("emailVerified").asBoolean()).isTrue();
        assertThat(emailVerifyRes.get("data").get("user").get("status").asText()).isEqualTo("ACTIVE");

        String accessToken = emailVerifyRes.get("data").get("tokens").get("accessToken").asText();

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
                .andExpect(jsonPath("$.data.phoneNumber").value("+919876543210"))
                .andExpect(jsonPath("$.data.collegeId").value(collegeId.toString()));
    }

    @Test
    @Transactional
    void communityRegistrationUsesEmailOtpAndHasNoCollegeAssociation() throws Exception {
        String email = "community-" + UUID.randomUUID() + "@example.com";
        CommunityRegistrationRequest request = new CommunityRegistrationRequest(
                email, "Community User", cityId, "+919876543210", "community password");

        JsonNode registration = json(mockMvc.perform(post("/api/v1/auth/register/community")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.emailOtp").exists())
                .andExpect(jsonPath("$.data.phoneOtp").doesNotExist())
                .andReturn().getResponse().getContentAsString());

        // Verify phone number is saved in DB
        User userInDb = userRepository.findByEmail(email).orElseThrow();
        assertThat(userInDb.getPhoneNumber()).isEqualTo("+919876543210");
        assertThat(userInDb.isEmailVerified()).isFalse();

        String emailChallengeId = registration.get("data").get("emailOtp").get("challengeId").asText();

        // Verify that ONLY an EMAIL OTP message was dispatched
        assertThat(otpGateway.messages()).hasSize(1);
        OtpDeliveryMessage emailMsg = otpGateway.messages().get(0);
        assertThat(emailMsg.channel().name()).isEqualTo("EMAIL");

        // Verify Email OTP -> activates account and issues tokens
        JsonNode emailVerifyRes = verifyResponse(emailMsg.code(), emailChallengeId);
        String accessToken = emailVerifyRes.get("data").get("tokens").get("accessToken").asText();
        assertThat(accessToken).isNotBlank();
        assertThat(emailVerifyRes.get("data").get("user").get("emailVerified").asBoolean()).isTrue();
        assertThat(emailVerifyRes.get("data").get("user").get("status").asText()).isEqualTo("ACTIVE");

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountType").value("COMMUNITY"))
                .andExpect(jsonPath("$.data.emailVerified").value(true))
                .andExpect(jsonPath("$.data.phoneNumber").value("+919876543210"))
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
                .andExpect(jsonPath("$.error.code").value(ErrorCode.BUSINESS_RULE_VIOLATION.name()))
                .andExpect(jsonPath("$.message").value("The email domain is not registered for the selected college."));
        assertThat(otpGateway.messages()).isEmpty();
    }

    @Test
    @Transactional
    void collegeWithNoConfiguredDomainIsSafelyBlocked() throws Exception {
        City city = entityManager.find(City.class, cityId);
        College unconfiguredCollege = new College("Unconfigured College", city);
        entityManager.persist(unconfiguredCollege);
        entityManager.flush();

        StudentRegistrationRequest request = new StudentRegistrationRequest(
                cityId, unconfiguredCollege.getId(), "student@gmail.com", "Student User", "password123");

        mockMvc.perform(post("/api/v1/auth/register/student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value(ErrorCode.BUSINESS_RULE_VIOLATION.name()))
                .andExpect(jsonPath("$.message").value("Official email verification for this college is not configured yet. Please try again later."));
        assertThat(otpGateway.messages()).isEmpty();
    }

    @Test
    @Transactional
    void bennettAndShardaUniversityDomainValidation() throws Exception {
        UUID bennettId = UUID.fromString("c0000001-0000-0000-0000-000000000001");
        UUID shardaId = UUID.fromString("c0000001-0000-0000-0000-000000000004");
        UUID greaterNoidaId = UUID.fromString("d0000001-0000-0000-0000-000000000004");

        // Bennett + @bennett.edu.in -> accepted
        mockMvc.perform(post("/api/v1/auth/register/student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new StudentRegistrationRequest(
                                greaterNoidaId, bennettId, "student.one@bennett.edu.in", "Bennett Student", "password123"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING_VERIFICATION"));

        // Bennett + @gmail.com -> rejected
        mockMvc.perform(post("/api/v1/auth/register/student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new StudentRegistrationRequest(
                                greaterNoidaId, bennettId, "student.one@gmail.com", "Bennett Student", "password123"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("The email domain is not registered for the selected college."));

        // Sharda + @ug.sharda.ac.in -> accepted
        mockMvc.perform(post("/api/v1/auth/register/student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new StudentRegistrationRequest(
                                greaterNoidaId, shardaId, "student.ug@ug.sharda.ac.in", "Sharda UG Student", "password123"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING_VERIFICATION"));

        // Sharda + @pg.sharda.ac.in -> accepted
        mockMvc.perform(post("/api/v1/auth/register/student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new StudentRegistrationRequest(
                                greaterNoidaId, shardaId, "student.pg@pg.sharda.ac.in", "Sharda PG Student", "password123"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING_VERIFICATION"));

        // Sharda + @gmail.com -> rejected
        mockMvc.perform(post("/api/v1/auth/register/student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new StudentRegistrationRequest(
                                greaterNoidaId, shardaId, "student.sharda@gmail.com", "Sharda Student", "password123"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("The email domain is not registered for the selected college."));

        // Bennett email + wrong college (Sharda ID) -> rejected
        mockMvc.perform(post("/api/v1/auth/register/student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new StudentRegistrationRequest(
                                greaterNoidaId, shardaId, "student.bennett@bennett.edu.in", "Student", "password123"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("The email domain is not registered for the selected college."));

        // Bennett email + Bennett college + wrong city (Delhi ID) -> rejected (selected college not in selected city)
        UUID delhiId = UUID.fromString("d0000001-0000-0000-0000-000000000001");
        mockMvc.perform(post("/api/v1/auth/register/student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new StudentRegistrationRequest(
                                delhiId, bennettId, "student.bennett@bennett.edu.in", "Student", "password123"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("The selected college is not in the selected city."));
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

    @Test
    @Transactional
    void repeatedInvalidLoginAttemptsAreRateLimited() throws Exception {
        JsonNode registration = registerStudent();
        String email = entityManager.createQuery("select user.email from User user where user.id = :id", String.class)
                .setParameter("id", UUID.fromString(registration.get("data").get("userId").asText()))
                .getSingleResult();

        for (int attempt = 1; attempt <= 5; attempt++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsBytes(new LoginRequest(email, "wrong password"))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.code").value(ErrorCode.INVALID_CREDENTIALS.name()));
        }

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new LoginRequest(email, "wrong password"))))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value(ErrorCode.LOGIN_RATE_LIMITED.name()));
    }

    @Test
    @Transactional
    void pendingVerificationAccountLoginReturnsChallengesWithoutTokensAndAllowsRecovery() throws Exception {
        String domain = entityManager.createQuery(
                        "select domain.domain from CollegeEmailDomain domain where domain.college.id = :collegeId",
                        String.class)
                .setParameter("collegeId", collegeId)
                .getSingleResult();
        String email = "pending-recovery-" + UUID.randomUUID() + "@" + domain;
        String password = "secureRecoveryPassword123";

        // 1. Register student with phone
        mockMvc.perform(post("/api/v1/auth/register/student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new StudentRegistrationRequest(
                                cityId, collegeId, email, "Recovery Student", "+919876543210", password))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING_VERIFICATION"));

        // 2. Try login with wrong password -> rejected
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new LoginRequest(email, "wrongPassword"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value(ErrorCode.INVALID_CREDENTIALS.name()));

        // 3. Login with correct password -> returns PENDING_VERIFICATION, no tokens, non-secret email challenge metadata
        JsonNode loginRes = json(mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_VERIFICATION"))
                .andExpect(jsonPath("$.data.tokens").doesNotExist())
                .andExpect(jsonPath("$.data.accessToken").doesNotExist())
                .andExpect(jsonPath("$.data.emailOtp").exists())
                .andExpect(jsonPath("$.data.emailOtp.code").doesNotExist())
                .andExpect(jsonPath("$.data.phoneOtp").doesNotExist())
                .andReturn().getResponse().getContentAsString());

        String emailChallengeId = loginRes.get("data").get("emailOtp").get("challengeId").asText();

        OtpDeliveryMessage emailMsg = otpGateway.messages().stream()
                .filter(m -> m.channel().name().equals("EMAIL")).reduce((first, second) -> second).orElseThrow();

        // 4. Verify email alone -> account immediately activates!
        JsonNode emailVerifyRes = verifyResponse(emailMsg.code(), emailChallengeId);
        assertThat(emailVerifyRes.get("data").get("tokens").get("accessToken").asText()).isNotBlank();
        assertThat(emailVerifyRes.get("data").get("user").get("emailVerified").asBoolean()).isTrue();
        assertThat(emailVerifyRes.get("data").get("user").get("status").asText()).isEqualTo("ACTIVE");

        // 5. Next login returns ACTIVE with tokens
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
    }

    @Test
    @Transactional
    void phoneVerifiedFalseDoesNotPreventActivationAndLogin() throws Exception {
        String domain = entityManager.createQuery(
                        "select domain.domain from CollegeEmailDomain domain where domain.college.id = :collegeId",
                        String.class)
                .setParameter("collegeId", collegeId)
                .getSingleResult();
        String email = "unverified-phone-" + UUID.randomUUID() + "@" + domain;
        String password = "securePassword123";

        StudentRegistrationRequest request = new StudentRegistrationRequest(
                cityId, collegeId, email, "Phone User", "+919876543210", password);

        JsonNode reg = json(mockMvc.perform(post("/api/v1/auth/register/student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());

        String challengeId = reg.get("data").get("emailOtp").get("challengeId").asText();
        OtpDeliveryMessage emailMsg = otpGateway.last();

        // Verify email OTP alone
        JsonNode verifyRes = verifyResponse(emailMsg.code(), challengeId);
        assertThat(verifyRes.get("data").get("tokens").get("accessToken").asText()).isNotBlank();
        assertThat(verifyRes.get("data").get("user").get("status").asText()).isEqualTo("ACTIVE");
        assertThat(verifyRes.get("data").get("user").get("emailVerified").asBoolean()).isTrue();
        assertThat(verifyRes.get("data").get("user").get("phoneVerified").asBoolean()).isFalse();

        // Directly verify User entity state in database
        User user = userRepository.findByEmail(email).orElseThrow();
        assertThat(user.getPhoneNumber()).isEqualTo("+919876543210");
        assertThat(user.isPhoneVerified()).isFalse();
        assertThat(user.isEmailVerified()).isTrue();
        assertThat(user.getStatus()).isEqualTo(com.campuscart.user.domain.AccountStatus.ACTIVE);
    }

    @Test
    @Transactional
    void loginEnforcesAccountTypeMatching() throws Exception {
        // Register & activate a student
        String domain = entityManager.createQuery(
                        "select domain.domain from CollegeEmailDomain domain where domain.college.id = :collegeId",
                        String.class)
                .setParameter("collegeId", collegeId)
                .getSingleResult();
        String studentEmail = "student-login-" + UUID.randomUUID() + "@" + domain;
        StudentRegistrationRequest studentReq = new StudentRegistrationRequest(
                cityId, collegeId, studentEmail, "Student Tester", "StudentPass123");
        JsonNode studentReg = json(mockMvc.perform(post("/api/v1/auth/register/student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(studentReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
        String studentChallengeId = studentReg.get("data").get("emailOtp").get("challengeId").asText();
        verify(otpGateway.last().code(), studentChallengeId);

        // Register & activate a community user
        String communityEmail = "community-login-" + UUID.randomUUID() + "@example.com";
        CommunityRegistrationRequest commReq = new CommunityRegistrationRequest(
                communityEmail, "Community Tester", cityId, "+919876543210", "CommPass123");
        JsonNode commReg = json(mockMvc.perform(post("/api/v1/auth/register/community")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(commReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
        String commChallengeId = commReg.get("data").get("emailOtp").get("challengeId").asText();
        verify(otpGateway.last().code(), commChallengeId);

        // 1. Student authenticates via Student login portal -> SUCCESS
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new LoginRequest(
                                studentEmail, "StudentPass123", com.campuscart.user.domain.UserType.STUDENT))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());

        // 2. Community authenticates via Community login portal -> SUCCESS
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new LoginRequest(
                                communityEmail, "CommPass123", com.campuscart.user.domain.UserType.COMMUNITY))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());

        // 3. Student attempts login via Community portal -> REJECTED (401)
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new LoginRequest(
                                studentEmail, "StudentPass123", com.campuscart.user.domain.UserType.COMMUNITY))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value(ErrorCode.INVALID_CREDENTIALS.name()));

        // 4. Community attempts login via Student portal -> REJECTED (401)
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new LoginRequest(
                                communityEmail, "CommPass123", com.campuscart.user.domain.UserType.STUDENT))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value(ErrorCode.INVALID_CREDENTIALS.name()));
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
