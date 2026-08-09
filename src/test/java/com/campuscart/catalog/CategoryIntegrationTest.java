package com.campuscart.catalog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campuscart.catalog.dto.CategoryRequest;
import com.campuscart.security.JwtService;
import com.campuscart.support.AbstractMySqlIntegrationTest;
import com.campuscart.user.domain.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class CategoryIntegrationTest extends AbstractMySqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Test
    void onlyAdminCanMutateCategories() throws Exception {
        String adminToken = jwtService.generateAccessToken(UUID.randomUUID(), "admin@example.com", Role.ADMIN);
        String studentToken = jwtService.generateAccessToken(UUID.randomUUID(), "student@example.com", Role.STUDENT);
        CategoryRequest request = new CategoryRequest("Stationery-" + UUID.randomUUID(), "stationery-" + UUID.randomUUID());

        mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isForbidden());

        String response = mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String categoryId = objectMapper.readTree(response).get("data").get("id").asText();

        mockMvc.perform(delete("/api/v1/categories/{id}", categoryId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/categories/{id}", categoryId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }
}
