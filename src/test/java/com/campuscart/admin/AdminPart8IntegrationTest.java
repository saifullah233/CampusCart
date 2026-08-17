package com.campuscart.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campuscart.catalog.domain.Category;
import com.campuscart.catalog.dto.CategoryRequest;
import com.campuscart.college.domain.College;
import com.campuscart.college.dto.CollegeRequest;
import com.campuscart.common.exception.ErrorCode;
import com.campuscart.location.domain.City;
import com.campuscart.location.dto.CityRequest;
import com.campuscart.product.domain.ProductType;
import com.campuscart.product.domain.SellingReach;
import com.campuscart.product.dto.CreateProductRequest;
import com.campuscart.security.JwtService;
import com.campuscart.support.AbstractMySqlIntegrationTest;
import com.campuscart.user.domain.Role;
import com.campuscart.user.domain.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@AutoConfigureMockMvc
class AdminPart8IntegrationTest extends AbstractMySqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtService jwtService;

    private City city;
    private College college;
    private Category category;
    private User seller;
    private User student;
    private User admin;
    private String sellerToken;
    private String studentToken;
    private String adminToken;

    @BeforeEach
    void seed() {
        String suffix = UUID.randomUUID().toString();
        city = new City("Admin City-" + suffix, "Maharashtra");
        college = new College("Admin College-" + suffix, city);
        category = new Category("Admin Category-" + suffix, "admin-category-" + suffix);
        entityManager.persist(city);
        entityManager.persist(college);
        entityManager.persist(category);

        seller = activeStudent("admin-seller-" + suffix + "@seller.edu", "Seller", college);
        student = activeStudent("admin-student-" + suffix + "@student.edu", "Student", college);
        admin = activeStudent("admin-operator-" + suffix + "@admin.edu", "Admin", college);
        entityManager.persist(seller);
        entityManager.persist(student);
        entityManager.persist(admin);
        entityManager.flush();
        jdbcTemplate.update("UPDATE users SET role = 'ADMIN' WHERE id = UUID_TO_BIN(?)", admin.getId().toString());
        entityManager.clear();

        sellerToken = jwtService.generateAccessToken(seller);
        studentToken = jwtService.generateAccessToken(student);
        adminToken = jwtService.generateAccessToken(admin.getId(), admin.getEmail(), Role.ADMIN);
    }

    @Test
    @Transactional
    void adminEndpointsEnforceAuthenticationAndRole() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/admin/dashboard").header("Authorization", bearer(studentToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/dashboard").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalUsers").isNumber())
                .andExpect(jsonPath("$.data.totalProducts").isNumber())
                .andExpect(jsonPath("$.data.totalOrders").isNumber())
                .andExpect(jsonPath("$.data.totalReports").isNumber());
        mockMvc.perform(get("/api/v1/admin/analytics").header("Authorization", bearer(studentToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/users").header("Authorization", bearer(studentToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/audit-logs").header("Authorization", bearer(studentToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void adminManagesReferenceDataAndAuditLogs() throws Exception {
        JsonNode newCity = json(mockMvc.perform(post("/api/v1/admin/cities")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new CityRequest(
                                "Managed City-" + UUID.randomUUID(), "Karnataka"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
        String cityId = newCity.get("data").get("id").asText();

        JsonNode newCollege = json(mockMvc.perform(post("/api/v1/admin/colleges")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new CollegeRequest(
                                "Managed College-" + UUID.randomUUID(), UUID.fromString(cityId)))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
        String collegeId = newCollege.get("data").get("id").asText();

        mockMvc.perform(post("/api/v1/admin/cities/{id}/deactivate", cityId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));
        mockMvc.perform(post("/api/v1/admin/colleges/{id}/deactivate", collegeId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));

        JsonNode newCategory = json(mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new CategoryRequest(
                                "Managed Category-" + UUID.randomUUID(), "managed-category-" + UUID.randomUUID()))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
        String categoryId = newCategory.get("data").get("id").asText();
        mockMvc.perform(post("/api/v1/categories/{id}/deactivate", categoryId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));

        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(org.hamcrest.Matchers.greaterThan(0)));
    }

    @Test
    @Transactional
    void suspensionAndActivationAffectExistingAccessTokens() throws Exception {
        mockMvc.perform(post("/api/v1/admin/users/{id}/suspend", student.getId())
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUSPENDED"));
        mockMvc.perform(get("/api/v1/users/me").header("Authorization", bearer(studentToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value(ErrorCode.ACCOUNT_NOT_ACTIVE.name()));
        mockMvc.perform(get("/api/v1/reviews/products/{productId}", UUID.randomUUID())
                        .header("Authorization", bearer(studentToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value(ErrorCode.ACCOUNT_NOT_ACTIVE.name()));

        mockMvc.perform(post("/api/v1/admin/users/{id}/activate", student.getId())
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
        mockMvc.perform(get("/api/v1/users/me").header("Authorization", bearer(studentToken)))
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void productModerationQueueAndAnalyticsAreAdminOnly() throws Exception {
        String productId = createProduct();
        mockMvc.perform(get("/api/v1/admin/products").header("Authorization", bearer(studentToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/admin/products/{id}/hide", productId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));
        mockMvc.perform(get("/api/v1/admin/products").param("status", "INACTIVE")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(productId));
        mockMvc.perform(post("/api/v1/admin/products/{id}/restore", productId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
        mockMvc.perform(get("/api/v1/admin/analytics").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalProducts").isNumber())
                .andExpect(jsonPath("$.data.activeProducts").isNumber())
                .andExpect(jsonPath("$.data.soldProducts").isNumber())
                .andExpect(jsonPath("$.data.marketplaceActivity").isNumber());
    }

    private String createProduct() throws Exception {
        JsonNode response = json(mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", bearer(sellerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new CreateProductRequest(category.getId(),
                                "Moderated product-" + UUID.randomUUID(), "Product for admin moderation",
                                new BigDecimal("12.00"), ProductType.NEW, SellingReach.OUTSIDE_CAMPUS, 2))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
        return response.get("data").get("id").asText();
    }

    private User activeStudent(String email, String name, College college) {
        User user = new User(email, name, college);
        user.activateAfterEmailVerification();
        return user;
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private JsonNode json(String value) throws Exception {
        return objectMapper.readTree(value);
    }
}
