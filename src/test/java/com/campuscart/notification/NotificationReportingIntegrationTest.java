package com.campuscart.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campuscart.cart.dto.AddCartItemRequest;
import com.campuscart.chat.domain.ChatReportStatus;
import com.campuscart.chat.dto.ReportMessageRequest;
import com.campuscart.chat.dto.ReportProductRequest;
import com.campuscart.chat.dto.ReportUserRequest;
import com.campuscart.chat.dto.SendMessageRequest;
import com.campuscart.catalog.domain.Category;
import com.campuscart.college.domain.College;
import com.campuscart.common.exception.ErrorCode;
import com.campuscart.location.domain.City;
import com.campuscart.order.domain.OrderStatus;
import com.campuscart.order.dto.UpdateOrderStatusRequest;
import com.campuscart.product.domain.ProductType;
import com.campuscart.product.domain.SellingReach;
import com.campuscart.product.dto.CreateProductRequest;
import com.campuscart.security.JwtService;
import com.campuscart.support.AbstractMySqlIntegrationTest;
import com.campuscart.user.domain.Role;
import com.campuscart.user.domain.User;
import com.campuscart.user.dto.UpdateProfileRequest;
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
class NotificationReportingIntegrationTest extends AbstractMySqlIntegrationTest {

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
        City city = new City("Notification City-" + suffix, "Maharashtra");
        entityManager.persist(city);
        College sellerCollege = new College("Notification Seller College-" + suffix, city);
        College buyerCollege = new College("Notification Buyer College-" + suffix, city);
        entityManager.persist(sellerCollege);
        entityManager.persist(buyerCollege);
        category = new Category("Notification Books-" + suffix, "notification-books-" + suffix);
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
    void messageNotificationPersistsAndReadApiUpdatesUnreadState() throws Exception {
        String conversationId = startConversation(createProduct("Message"));
        sendMessage(conversationId, sellerToken, "Hello buyer");

        JsonNode notification = findNotification(buyerToken, "NEW_MESSAGE");
        assertThat(notification.get("dataJson").asText()).contains(conversationId);
        long unreadBefore = unreadCount(buyerToken);

        mockMvc.perform(patch("/api/v1/notifications/{id}/read", notification.get("id").asText())
                        .header("Authorization", bearer(buyerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.read").value(true));

        assertThat(unreadCount(buyerToken)).isEqualTo(unreadBefore - 1);
    }

    @Test
    @Transactional
    void orderNotificationsReachBuyerAndSeller() throws Exception {
        String productId = createProduct("Order");
        addToCart(productId);
        String orderId = json(mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", bearer(buyerToken)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("data").get("id").asText();

        assertThat(findNotification(sellerToken, "ORDER_RECEIVED").get("dataJson").asText())
                .contains(orderId);

        transition(orderId, sellerToken, OrderStatus.ACCEPTED);
        assertThat(findNotification(buyerToken, "ORDER_UPDATE").get("dataJson").asText())
                .contains("\"status\":\"ACCEPTED\"");
        assertThat(findNotification(sellerToken, "ORDER_UPDATE").get("dataJson").asText())
                .contains("\"status\":\"ACCEPTED\"");
    }

    @Test
    @Transactional
    void wishlistAndLikeNotificationsReachTheSeller() throws Exception {
        String productId = createProduct("Wishlist like");

        mockMvc.perform(post("/api/v1/wishlist/{productId}", productId)
                        .header("Authorization", bearer(buyerToken)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/products/{productId}/like", productId)
                        .header("Authorization", bearer(buyerToken)))
                .andExpect(status().isOk());

        assertThat(findNotification(sellerToken, "WISHLIST_ADDED").get("dataJson").asText())
                .contains(productId, buyer.getId().toString());
        assertThat(findNotification(sellerToken, "PRODUCT_LIKED").get("dataJson").asText())
                .contains(productId, buyer.getId().toString());
    }

    @Test
    @Transactional
    void newProductNotificationReachesEligibleUsersButNotSeller() throws Exception {
        String productId = createProduct("New product");

        assertThat(findNotification(buyerToken, "NEW_PRODUCT").get("dataJson").asText())
                .contains(productId);
        assertThat(notificationPage(sellerToken, 0, 20).get("totalElements").asLong()).isZero();
    }

    @Test
    @Transactional
    void profileUpdateCreatesAccountEventNotification() throws Exception {
        mockMvc.perform(patch("/api/v1/users/me")
                        .header("Authorization", bearer(buyerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new UpdateProfileRequest("Updated Buyer"))))
                .andExpect(status().isOk());

        assertThat(findNotification(buyerToken, "ACCOUNT_EVENT").get("dataJson").asText())
                .contains("PROFILE_UPDATED");
    }

    @Test
    @Transactional
    void notificationApisProvidePaginationReadAllAndOwnership() throws Exception {
        createProduct("First page");
        createProduct("Second page");
        JsonNode firstPage = notificationPage(buyerToken, 0, 1);
        assertThat(firstPage.get("size").asInt()).isEqualTo(1);
        assertThat(firstPage.get("totalElements").asLong()).isEqualTo(2);
        assertThat(firstPage.get("totalPages").asInt()).isEqualTo(2);

        String productId = createProduct("Seller notification");
        mockMvc.perform(post("/api/v1/wishlist/{productId}", productId)
                        .header("Authorization", bearer(buyerToken)))
                .andExpect(status().isCreated());
        JsonNode sellerNotification = findNotification(sellerToken, "WISHLIST_ADDED");

        mockMvc.perform(patch("/api/v1/notifications/{id}/read", sellerNotification.get("id").asText())
                        .header("Authorization", bearer(buyerToken)))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/notifications/read-all")
                        .header("Authorization", bearer(sellerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(1));
        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", bearer(sellerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(0));
    }

    @Test
    @Transactional
    void reportsConversationMessageUserAndAdminCanChangeStatus() throws Exception {
        String conversationId = startConversation(createProduct("Report"));
        String messageId = sendMessage(conversationId, sellerToken, "Reportable message");

        mockMvc.perform(post("/api/v1/conversations/{id}/report", conversationId)
                        .header("Authorization", bearer(buyerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new ReportMessageRequest("SPAM", "Conversation", null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.conversationId").value(conversationId));
        mockMvc.perform(post("/api/v1/conversations/{id}/report", conversationId)
                        .header("Authorization", bearer(buyerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new ReportMessageRequest(
                                "ABUSE", "Message", UUID.fromString(messageId)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.messageId").value(messageId));

        JsonNode userReport = json(mockMvc.perform(post("/api/v1/reports/users/{id}", seller.getId())
                        .header("Authorization", bearer(buyerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new ReportUserRequest("HARASSMENT", "User report"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.reportedUserId").value(seller.getId().toString()))
                .andReturn().getResponse().getContentAsString());

        mockMvc.perform(get("/api/v1/admin/chat-reports")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isNotEmpty());
        mockMvc.perform(patch("/api/v1/admin/chat-reports/{id}", userReport.get("data").get("id").asText())
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new com.campuscart.chat.dto.ReviewReportRequest(
                                ChatReportStatus.RESOLVED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESOLVED"));
    }

    @Test
    @Transactional
    void reportingRejectsUnauthorizedInvalidSelfAndDuplicateTargets() throws Exception {
        String conversationId = startConversation(createProduct("Report validation"));

        mockMvc.perform(post("/api/v1/conversations/{id}/report", conversationId)
                        .header("Authorization", bearer(strangerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new ReportMessageRequest("SPAM", "No access", null))))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/reports/users/{id}", UUID.randomUUID())
                        .header("Authorization", bearer(buyerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new ReportUserRequest("SPAM", "Missing target"))))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/reports/users/{id}", buyer.getId())
                        .header("Authorization", bearer(buyerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new ReportUserRequest("SPAM", "Self report"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value(ErrorCode.INVALID_REPORT.name()));
        mockMvc.perform(post("/api/v1/reports/users/{id}", seller.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new ReportUserRequest("SPAM", "Unauthenticated"))))
                .andExpect(status().isUnauthorized());

        JsonNode firstUserReport = reportUser(seller.getId());
        assertThat(firstUserReport.get("data").get("status").asText())
                .isEqualTo(ChatReportStatus.PENDING.name());
        mockMvc.perform(post("/api/v1/reports/users/{id}", seller.getId())
                        .header("Authorization", bearer(buyerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new ReportUserRequest("SPAM", "Duplicate"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value(ErrorCode.DUPLICATE_RESOURCE.name()));
    }

    @Test
    @Transactional
    void inactiveReporterCannotCreateUserReport() throws Exception {
        jdbcTemplate.update("UPDATE users SET status = 'SUSPENDED' WHERE id = UUID_TO_BIN(?)", buyer.getId().toString());
        entityManager.clear();

        mockMvc.perform(post("/api/v1/reports/users/{id}", seller.getId())
                        .header("Authorization", bearer(buyerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new ReportUserRequest("SPAM", "Inactive reporter"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value(ErrorCode.ACCOUNT_NOT_ACTIVE.name()));
    }

    @Test
    @Transactional
    void terminalUserReportsAllowNewReports() throws Exception {
        JsonNode first = reportUser(seller.getId());
        reviewReport(first.get("data").get("id").asText(), ChatReportStatus.RESOLVED);

        JsonNode second = reportUser(seller.getId());
        reviewReport(second.get("data").get("id").asText(), ChatReportStatus.DISMISSED);

        JsonNode third = reportUser(seller.getId());
        assertThat(third.get("data").get("status").asText()).isEqualTo(ChatReportStatus.PENDING.name());
    }

    @Test
    @Transactional
    void productReportsUseTheAdminLifecycleAndReportedQueue() throws Exception {
        String productId = createProduct("Reported product");
        JsonNode report = json(mockMvc.perform(post("/api/v1/reports/products/{id}", productId)
                        .header("Authorization", bearer(buyerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new ReportProductRequest("COUNTERFEIT", "Needs review"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.reportedProductId").value(productId))
                .andReturn().getResponse().getContentAsString());

        mockMvc.perform(get("/api/v1/admin/products/reported")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].reportedProductId").value(productId));

        String reportId = report.get("data").get("id").asText();
        reviewReport(reportId, ChatReportStatus.UNDER_REVIEW);
        mockMvc.perform(patch("/api/v1/admin/chat-reports/{id}", reportId)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new com.campuscart.chat.dto.ReviewReportRequest(
                                ChatReportStatus.PENDING))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value(ErrorCode.INVALID_REPORT.name()));
        reviewReport(reportId, ChatReportStatus.RESOLVED);

        mockMvc.perform(post("/api/v1/reports/products/{id}", productId)
                        .header("Authorization", bearer(buyerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new ReportProductRequest("COUNTERFEIT", "Second report"))))
                .andExpect(status().isCreated());
    }

    private User activeStudent(String email, String name, College college) {
        User user = new User(email, name, college);
        user.activateAfterEmailVerification();
        return user;
    }

    private String createProduct(String title) throws Exception {
        JsonNode response = json(mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", bearer(sellerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new CreateProductRequest(category.getId(),
                                title + UUID.randomUUID(), "Notification test product", new BigDecimal("20.00"),
                                ProductType.NEW, SellingReach.PUBLIC, 2))))
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

    private String sendMessage(String conversationId, String token, String content) throws Exception {
        JsonNode response = json(mockMvc.perform(post("/api/v1/conversations/{id}/messages", conversationId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new SendMessageRequest(content))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        return response.get("data").get("id").asText();
    }

    private void addToCart(String productId) throws Exception {
        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", bearer(buyerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(
                                new AddCartItemRequest(UUID.fromString(productId), 1))))
                .andExpect(status().isCreated());
    }

    private void transition(String orderId, String token, OrderStatus target) throws Exception {
        mockMvc.perform(patch("/api/v1/orders/{id}/status", orderId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new UpdateOrderStatusRequest(target))))
                .andExpect(status().isOk());
    }

    private JsonNode reportUser(UUID targetUserId) throws Exception {
        return json(mockMvc.perform(post("/api/v1/reports/users/{id}", targetUserId)
                        .header("Authorization", bearer(buyerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new ReportUserRequest("SPAM", "Duplicate test"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
    }

    private void reviewReport(String reportId, ChatReportStatus targetStatus) throws Exception {
        mockMvc.perform(patch("/api/v1/admin/chat-reports/{id}", reportId)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(
                                new com.campuscart.chat.dto.ReviewReportRequest(targetStatus))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(targetStatus.name()));
    }

    private JsonNode findNotification(String token, String type) throws Exception {
        JsonNode content = notificationPage(token, 0, 50).get("content");
        for (JsonNode notification : content) {
            if (type.equals(notification.get("type").asText())) {
                return notification;
            }
        }
        throw new AssertionError("Notification type not found: " + type);
    }

    private JsonNode notificationPage(String token, int page, int size) throws Exception {
        return json(mockMvc.perform(get("/api/v1/notifications")
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size))
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).get("data");
    }

    private long unreadCount(String token) throws Exception {
        return json(mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).get("data").asLong();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private JsonNode json(String value) throws Exception {
        return objectMapper.readTree(value);
    }
}
