package com.campuscart.chat;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campuscart.chat.dto.ReportMessageRequest;
import com.campuscart.chat.dto.ReviewReportRequest;
import com.campuscart.chat.dto.SendMessageRequest;
import com.campuscart.catalog.domain.Category;
import com.campuscart.college.domain.College;
import com.campuscart.location.domain.City;
import com.campuscart.order.domain.OrderStatus;
import com.campuscart.product.domain.ProductType;
import com.campuscart.product.domain.SellingReach;
import com.campuscart.product.dto.CreateProductRequest;
import com.campuscart.security.JwtService;
import com.campuscart.user.domain.Role;
import com.campuscart.user.domain.User;
import com.campuscart.support.AbstractMySqlIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@AutoConfigureMockMvc
class ChatIntegrationTest extends AbstractMySqlIntegrationTest {

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

    private User seller;
    private User buyer;
    private User stranger;
    private User admin;
    private Category category;
    private String sellerToken;
    private String buyerToken;
    private String strangerToken;
    private String adminToken;

    @BeforeEach
    void seedUsers() {
        String suffix = UUID.randomUUID().toString();
        City city = new City("Chat City-" + suffix, "Maharashtra");
        entityManager.persist(city);
        College sellerCollege = new College("Chat Seller College-" + suffix, city);
        College buyerCollege = new College("Chat Buyer College-" + suffix, city);
        entityManager.persist(sellerCollege);
        entityManager.persist(buyerCollege);
        category = new Category("Chat Books-" + suffix, "chat-books-" + suffix);
        entityManager.persist(category);

        seller = activeStudent("seller-" + suffix + "@seller.edu", "Seller", sellerCollege);
        buyer = activeStudent("buyer-" + suffix + "@buyer.edu", "Buyer", buyerCollege);
        stranger = activeStudent("stranger-" + suffix + "@stranger.edu", "Stranger", buyerCollege);
        admin = activeStudent("admin-" + suffix + "@admin.edu", "Admin", sellerCollege);
        entityManager.persist(seller);
        entityManager.persist(buyer);
        entityManager.persist(stranger);
        entityManager.persist(admin);
        entityManager.flush();
        jdbcTemplate.update("UPDATE users SET role = 'ADMIN' WHERE id = UUID_TO_BIN(?)", admin.getId().toString());
        entityManager.clear();

        sellerToken = jwtService.generateAccessToken(seller);
        buyerToken = jwtService.generateAccessToken(buyer);
        strangerToken = jwtService.generateAccessToken(stranger);
        adminToken = jwtService.generateAccessToken(admin.getId(), admin.getEmail(), Role.ADMIN);
    }

    @Test
    @Transactional
    void conversationAccessUnreadReadAndMessageNotificationAreProtected() throws Exception {
        String productId = createProduct();
        String conversationId = startConversation(productId);

        mockMvc.perform(get("/api/v1/conversations/{id}", conversationId)
                        .header("Authorization", bearer(strangerToken)))
                .andExpect(status().isForbidden());

        JsonNode message = json(mockMvc.perform(post("/api/v1/conversations/{id}/messages", conversationId)
                        .header("Authorization", bearer(sellerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new SendMessageRequest("Hello buyer"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        mockMvc.perform(get("/api/v1/conversations/{id}/unread-count", conversationId)
                        .header("Authorization", bearer(buyerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(1));
        mockMvc.perform(post("/api/v1/conversations/{id}/read", conversationId)
                        .header("Authorization", bearer(buyerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(1));
        mockMvc.perform(get("/api/v1/conversations/{id}/unread-count", conversationId)
                        .header("Authorization", bearer(buyerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(0));

        mockMvc.perform(get("/api/v1/notifications").header("Authorization", bearer(buyerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].type").value("NEW_MESSAGE"));

        mockMvc.perform(post("/api/v1/conversations/{id}/report", conversationId)
                        .header("Authorization", bearer(buyerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new ReportMessageRequest(
                                "HARASSMENT", "Please review this message.",
                                UUID.fromString(message.get("data").get("id").asText())))))
                .andExpect(status().isCreated());
    }

    @Test
    @Transactional
    void blockPreventsMessagingUntilUnblocked() throws Exception {
        String conversationId = startConversation(createProduct());
        mockMvc.perform(post("/api/v1/blocks/{id}", seller.getId())
                        .header("Authorization", bearer(buyerToken)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/conversations/{id}/messages", conversationId)
                        .header("Authorization", bearer(sellerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new SendMessageRequest("Blocked message"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("USER_BLOCKED"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(
                        "/api/v1/blocks/{id}", seller.getId())
                        .header("Authorization", bearer(buyerToken)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/conversations/{id}/messages", conversationId)
                        .header("Authorization", bearer(sellerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new SendMessageRequest("Allowed again"))))
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void adminCanReviewReportsAndLikeCreatesSellerNotification() throws Exception {
        String productId = createProduct();
        String conversationId = startConversation(productId);
        JsonNode report = json(mockMvc.perform(post("/api/v1/conversations/{id}/report", conversationId)
                        .header("Authorization", bearer(buyerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new ReportMessageRequest(
                                "SPAM", "Repeated solicitation", null))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());

        mockMvc.perform(get("/api/v1/admin/chat-reports")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].status").value("PENDING"));
        mockMvc.perform(patch("/api/v1/admin/chat-reports/{id}", report.get("data").get("id").asText())
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new ReviewReportRequest(
                                com.campuscart.chat.domain.ChatReportStatus.RESOLVED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESOLVED"));

        mockMvc.perform(post("/api/v1/products/{id}/like", productId)
                        .header("Authorization", bearer(buyerToken)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/notifications").header("Authorization", bearer(sellerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].type").value("PRODUCT_LIKED"));
    }

    @Test
    @Transactional
    void unsafeContactContentIsRejected() throws Exception {
        String conversationId = startConversation(createProduct());
        mockMvc.perform(post("/api/v1/conversations/{id}/messages", conversationId)
                        .header("Authorization", bearer(buyerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new SendMessageRequest(
                                "Reach me at buyer@example.com"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("UNSAFE_CONTENT"));
    }

    private User activeStudent(String email, String name, College college) {
        User user = new User(email, name, college);
        user.activateAfterEmailVerification();
        return user;
    }

    private String createProduct() throws Exception {
        JsonNode response = json(mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", bearer(sellerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new CreateProductRequest(category.getId(),
                                "Chat product " + UUID.randomUUID(), "Product used in chat tests",
                                new BigDecimal("20.00"), ProductType.NEW, SellingReach.OUTSIDE_CAMPUS, 1))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
        return response.get("data").get("id").asText();
    }

    private String startConversation(String productId) throws Exception {
        JsonNode response = json(mockMvc.perform(post("/api/v1/conversations")
                        .param("productId", productId)
                        .header("Authorization", bearer(buyerToken)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
        return response.get("data").get("id").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private JsonNode json(String value) throws Exception {
        return objectMapper.readTree(value);
    }
}
