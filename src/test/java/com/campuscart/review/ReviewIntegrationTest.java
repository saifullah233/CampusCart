package com.campuscart.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campuscart.cart.dto.AddCartItemRequest;
import com.campuscart.catalog.domain.Category;
import com.campuscart.college.domain.College;
import com.campuscart.common.exception.ErrorCode;
import com.campuscart.location.domain.City;
import com.campuscart.order.domain.Order;
import com.campuscart.order.domain.OrderItem;
import com.campuscart.order.domain.OrderStatus;
import com.campuscart.order.dto.UpdateOrderStatusRequest;
import com.campuscart.product.domain.Product;
import com.campuscart.product.domain.ProductType;
import com.campuscart.product.domain.SellingReach;
import com.campuscart.product.dto.CreateProductRequest;
import com.campuscart.review.domain.ReviewStatus;
import com.campuscart.review.dto.CreateReviewRequest;
import com.campuscart.review.dto.ReviewModerationRequest;
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
class ReviewIntegrationTest extends AbstractMySqlIntegrationTest {

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
        City city = new City("Review City-" + suffix, "Maharashtra");
        entityManager.persist(city);
        College sellerCollege = new College("Review Seller College-" + suffix, city);
        College buyerCollege = new College("Review Buyer College-" + suffix, city);
        entityManager.persist(sellerCollege);
        entityManager.persist(buyerCollege);
        category = new Category("Review Books-" + suffix, "review-books-" + suffix);
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
    void completedBuyerCanSubmitReviewAndAdminCanModerateVisibility() throws Exception {
        String productId = createProduct("Completed review");
        String orderId = completedOrder(productId);

        JsonNode review = createReview(buyerToken, orderId, productId, 5, "Excellent seller.");
        String reviewId = review.get("data").get("id").asText();
        assertThat(review.get("data").get("reviewerId").asText()).isEqualTo(buyer.getId().toString());
        assertThat(review.get("data").get("reviewedUserId").asText()).isEqualTo(seller.getId().toString());
        assertThat(review.get("data").get("status").asText()).isEqualTo("PENDING");

        mockMvc.perform(get("/api/v1/reviews/products/{productId}", productId)
                        .header("Authorization", bearer(buyerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));

        moderate(reviewId, ReviewStatus.APPROVED);
        mockMvc.perform(get("/api/v1/reviews/products/{productId}", productId)
                        .header("Authorization", bearer(buyerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));
        mockMvc.perform(get("/api/v1/reviews/sellers/{sellerId}", seller.getId())
                        .header("Authorization", bearer(buyerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(reviewId));

        moderate(reviewId, ReviewStatus.HIDDEN);
        mockMvc.perform(get("/api/v1/reviews/products/{productId}", productId)
                        .header("Authorization", bearer(buyerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));
        moderate(reviewId, ReviewStatus.APPROVED);
    }

    @Test
    @Transactional
    void reviewRejectsIncompleteUnrelatedDuplicateAndInvalidRequests() throws Exception {
        String productId = createProduct("Review validation");
        String orderId = placeOrder(productId);

        reviewRequest(buyerToken, orderId, productId, 5, "Too early")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value(ErrorCode.INVALID_REVIEW.name()));
        completeOrder(orderId);

        reviewRequest(strangerToken, orderId, productId, 5, "Not my order")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value(ErrorCode.INVALID_REVIEW.name()));
        reviewRequest(buyerToken, orderId, UUID.randomUUID().toString(), 5, "Wrong product")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value(ErrorCode.INVALID_REVIEW.name()));
        reviewRequest(buyerToken, orderId, productId, 0, "Invalid rating")
                .andExpect(status().isBadRequest());
        reviewRequest(buyerToken, orderId, productId, 5, "   ")
                .andExpect(status().isBadRequest());

        createReview(buyerToken, orderId, productId, 4, "Valid review.");
        reviewRequest(buyerToken, orderId, productId, 4, "Duplicate review")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value(ErrorCode.DUPLICATE_RESOURCE.name()));
    }

    @Test
    @Transactional
    void reviewRejectsSelfReviewAndUnauthenticatedSubmission() throws Exception {
        String productId = createProduct("Self review");
        Product product = entityManager.find(Product.class, UUID.fromString(productId));
        Order selfOrder = new Order(seller, new BigDecimal("20.00"));
        selfOrder.transitionTo(OrderStatus.ACCEPTED);
        selfOrder.transitionTo(OrderStatus.SHIPPED);
        selfOrder.transitionTo(OrderStatus.DELIVERED);
        selfOrder.transitionTo(OrderStatus.COMPLETED);
        entityManager.persist(selfOrder);
        entityManager.flush();
        entityManager.persist(new OrderItem(selfOrder, product, 1));
        entityManager.flush();

        reviewRequest(sellerToken, selfOrder.getId().toString(), productId, 5, "Self review")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value(ErrorCode.INVALID_REVIEW.name()));
        mockMvc.perform(post("/api/v1/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new CreateReviewRequest(
                                selfOrder.getId(), UUID.fromString(productId), 5, "No token"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Transactional
    void reviewModerationRequiresAdmin() throws Exception {
        String productId = createProduct("Moderation auth");
        String orderId = completedOrder(productId);
        String reviewId = createReview(buyerToken, orderId, productId, 5, "Moderate me")
                .get("data").get("id").asText();

        mockMvc.perform(get("/api/v1/admin/reviews"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/admin/reviews").header("Authorization", bearer(buyerToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/v1/admin/reviews/{reviewId}", reviewId)
                        .header("Authorization", bearer(buyerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new ReviewModerationRequest(ReviewStatus.APPROVED))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/reviews").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(reviewId));
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
                                title + UUID.randomUUID(), "Review test product", new BigDecimal("20.00"),
                                ProductType.NEW, SellingReach.OUTSIDE_CAMPUS, 2))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
        return response.get("data").get("id").asText();
    }

    private String placeOrder(String productId) throws Exception {
        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", bearer(buyerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(
                                new AddCartItemRequest(UUID.fromString(productId), 1))))
                .andExpect(status().isCreated());
        return json(mockMvc.perform(post("/api/v1/orders").header("Authorization", bearer(buyerToken)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("data").get("id").asText();
    }

    private String completedOrder(String productId) throws Exception {
        String orderId = placeOrder(productId);
        completeOrder(orderId);
        return orderId;
    }

    private void completeOrder(String orderId) throws Exception {
        transition(orderId, sellerToken, OrderStatus.ACCEPTED);
        transition(orderId, sellerToken, OrderStatus.SHIPPED);
        transition(orderId, sellerToken, OrderStatus.DELIVERED);
        transition(orderId, buyerToken, OrderStatus.COMPLETED);
    }

    private void transition(String orderId, String token, OrderStatus status) throws Exception {
        mockMvc.perform(patch("/api/v1/orders/{orderId}/status", orderId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new UpdateOrderStatusRequest(status))))
                .andExpect(status().isOk());
    }

    private JsonNode createReview(String token, String orderId, String productId, int rating, String text)
            throws Exception {
        return json(reviewRequest(token, orderId, productId, rating, text)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
    }

    private org.springframework.test.web.servlet.ResultActions reviewRequest(
            String token, String orderId, String productId, int rating, String text) throws Exception {
        return mockMvc.perform(post("/api/v1/reviews")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(new CreateReviewRequest(
                        UUID.fromString(orderId), UUID.fromString(productId), rating, text))));
    }

    private void moderate(String reviewId, ReviewStatus targetStatus) throws Exception {
        mockMvc.perform(patch("/api/v1/admin/reviews/{reviewId}", reviewId)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new ReviewModerationRequest(targetStatus))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(targetStatus.name()));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private JsonNode json(String value) throws Exception {
        return objectMapper.readTree(value);
    }
}
