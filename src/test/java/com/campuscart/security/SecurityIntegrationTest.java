package com.campuscart.security;

import com.campuscart.support.AbstractMySqlIntegrationTest;
import com.campuscart.user.domain.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(SecurityTestController.class)
class SecurityIntegrationTest extends AbstractMySqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Test
    void publicPingRemainsAccessible() throws Exception {
        mockMvc.perform(get("/api/v1/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void protectedEndpointUsesStandardUnauthorizedEnvelope() throws Exception {
        mockMvc.perform(get("/test/security/protected"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.error.detail").value("Authentication is required to access this resource."));
    }

    @Test
    void invalidBearerTokenIsNotAccepted() throws Exception {
        mockMvc.perform(get("/test/security/protected")
                        .header("Authorization", "Bearer definitely-not-a-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void verifiedPrincipalReachesMethodSecuredController() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateAccessToken(userId, "student@example.edu", Role.STUDENT);

        mockMvc.perform(get("/test/security/protected")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(userId.toString()))
                .andExpect(jsonPath("$.data.role").value("STUDENT"));
    }

    @Test
    void roleAuthorizationReturnsStandardForbiddenEnvelope() throws Exception {
        String token = jwtService.generateAccessToken(
                UUID.randomUUID(), "student@example.edu", Role.STUDENT);

        mockMvc.perform(get("/test/security/admin")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    @Test
    void urlRoleAuthorizationUsesSecurityAccessDeniedHandler() throws Exception {
        String token = jwtService.generateAccessToken(
                UUID.randomUUID(), "student@example.edu", Role.STUDENT);

        mockMvc.perform(get("/api/v1/admin/security-test")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    @Test
    void configuredCorsAllowsKnownOriginAndDoesNotUseWildcard() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options(
                                "/test/security/protected")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
    }

    @Test
    void invalidPathParameterUsesStandardMalformedRequestEnvelope() throws Exception {
        String token = jwtService.generateAccessToken(
                UUID.randomUUID(), "student@example.edu", Role.STUDENT);

        mockMvc.perform(get("/test/security/resources/not-a-uuid")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("MALFORMED_REQUEST"));
    }
}
