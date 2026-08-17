package com.campuscart.commerce;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import com.campuscart.order.domain.OrderStatus;
import com.campuscart.order.dto.UpdateOrderStatusRequest;
import com.campuscart.product.domain.ProductType;
import com.campuscart.product.domain.SellingReach;
import com.campuscart.product.dto.CreateProductRequest;
import com.campuscart.security.JwtService;
import com.campuscart.support.AbstractMySqlIntegrationTest;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@AutoConfigureMockMvc
class CommerceIntegrationTest extends AbstractMySqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JwtService jwtService;

    private User seller;
    private User buyer;
    private User stranger;
    private Category category;
    private String sellerToken;
    private String buyerToken;
    private String strangerToken;

    @BeforeEach
    void seedUsers() {
        String suffix = UUID.randomUUID().toString();
        City city = new City("Commerce City-" + suffix, "Maharashtra");
        entityManager.persist(city);
        College sellerCollege = new College("Seller College-" + suffix, city);
        College buyerCollege = new College("Buyer College-" + suffix, city);
        entityManager.persist(sellerCollege);
        entityManager.persist(buyerCollege);
        category = new Category("Commerce Books-" + suffix, "commerce-books-" + suffix);
        entityManager.persist(category);

        seller = new User("seller-" + suffix + "@seller.edu", "Seller", sellerCollege);
        buyer = new User("buyer-" + suffix + "@buyer.edu", "Buyer", buyerCollege);
        stranger = new User("stranger-" + suffix + "@stranger.edu", "Stranger", buyerCollege);
        seller.activateAfterEmailVerification();
        buyer.activateAfterEmailVerification();
        stranger.activateAfterEmailVerification();
        entityManager.persist(seller);
        entityManager.persist(buyer);
        entityManager.persist(stranger);
        entityManager.flush();

        sellerToken = jwtService.generateAccessToken(seller);
        buyerToken = jwtService.generateAccessToken(buyer);
        strangerToken = jwtService.generateAccessToken(stranger);
    }

    @Test
    @Transactional
    void wishlistPreventsDuplicatesAndSupportsCheckAndRemove() throws Exception {
        String productId = createProduct("Wishlist", ProductType.NEW, 2);

        mockMvc.perform(post("/api/v1/wishlist/{productId}", productId)
                        .header("Authorization", bearer(buyerToken)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.productId").value(productId));

        mockMvc.perform(post("/api/v1/wishlist/{productId}", productId)
                        .header("Authorization", bearer(buyerToken)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value(ErrorCode.DUPLICATE_RESOURCE.name()));

        mockMvc.perform(get("/api/v1/wishlist/check/{productId}", productId)
                        .header("Authorization", bearer(buyerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));

        mockMvc.perform(delete("/api/v1/wishlist/{productId}", productId)
                        .header("Authorization", bearer(buyerToken)))
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void cartValidatesQuantityAndBlocksUnavailableProductAtCheckout() throws Exception {
        String productId = createProduct("Cart", ProductType.SECOND_HAND, 2);
        AddCartItemRequest addOne = new AddCartItemRequest(UUID.fromString(productId), 1);

        addToCart(addOne).andExpect(status().isCreated());
        addToCart(addOne).andExpect(status().isCreated());
        addToCart(addOne).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value(ErrorCode.PRODUCT_UNAVAILABLE.name()));

        mockMvc.perform(get("/api/v1/cart").header("Authorization", bearer(buyerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.checkoutAvailable").value(true))
                .andExpect(jsonPath("$.data.items[0].quantity").value(2));

        mockMvc.perform(post("/api/v1/products/{id}/sold", productId)
                        .header("Authorization", bearer(sellerToken)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/orders").header("Authorization", bearer(buyerToken)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value(ErrorCode.PRODUCT_UNAVAILABLE.name()));
    }

    @Test
    @Transactional
    void orderAccessAndLifecycleAreAuthorizedAndPaymentRemainsDeferred() throws Exception {
        String productId = createProduct("Order", ProductType.NEW, 2);
        addToCart(new AddCartItemRequest(UUID.fromString(productId), 1)).andExpect(status().isCreated());

        JsonNode placed = json(mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", bearer(buyerToken)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PLACED"))
                .andExpect(jsonPath("$.data.paymentStatus").value("NOT_CONNECTED"))
                .andReturn().getResponse().getContentAsString());
        String orderId = placed.get("data").get("id").asText();

        mockMvc.perform(get("/api/v1/orders/{id}", orderId)
                        .header("Authorization", bearer(strangerToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value(ErrorCode.ACCESS_DENIED.name()));
        mockMvc.perform(patch("/api/v1/orders/{id}/status", orderId)
                        .header("Authorization", bearer(strangerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new UpdateOrderStatusRequest(OrderStatus.CANCELLED))))
                .andExpect(status().isForbidden());

        transition(orderId, sellerToken, OrderStatus.ACCEPTED);
        transition(orderId, sellerToken, OrderStatus.SHIPPED);
        transition(orderId, sellerToken, OrderStatus.DELIVERED);
        transition(orderId, buyerToken, OrderStatus.COMPLETED);

        mockMvc.perform(post("/api/v1/payments/orders/{id}/initialize", orderId)
                        .header("Authorization", bearer(buyerToken)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error.code").value(ErrorCode.PAYMENT_INTEGRATION_UNAVAILABLE.name()));
    }

    @Test
    @Transactional
    void cancellationRestoresReservedQuantity() throws Exception {
        String productId = createProduct("Cancel", ProductType.SECOND_HAND, 1);
        addToCart(new AddCartItemRequest(UUID.fromString(productId), 1)).andExpect(status().isCreated());
        JsonNode placed = json(mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", bearer(buyerToken)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());

        mockMvc.perform(post("/api/v1/orders/{id}/cancel", placed.get("data").get("id").asText())
                        .header("Authorization", bearer(buyerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        mockMvc.perform(get("/api/v1/products/{id}", productId)
                        .header("Authorization", bearer(sellerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.quantity").value(1));
    }

    private String createProduct(String title, ProductType type, int quantity) throws Exception {
        CreateProductRequest request = new CreateProductRequest(category.getId(), title + UUID.randomUUID(),
                "Commerce test product", new BigDecimal("12.50"), type, SellingReach.OUTSIDE_CAMPUS, quantity);
        JsonNode response = json(mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", bearer(sellerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
        return response.get("data").get("id").asText();
    }

    private org.springframework.test.web.servlet.ResultActions addToCart(AddCartItemRequest request)
            throws Exception {
        return mockMvc.perform(post("/api/v1/cart/items")
                .header("Authorization", bearer(buyerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)));
    }

    private void transition(String orderId, String token, OrderStatus status) throws Exception {
        mockMvc.perform(patch("/api/v1/orders/{id}/status", orderId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new UpdateOrderStatusRequest(status))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(status.name()));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private JsonNode json(String value) throws Exception {
        return objectMapper.readTree(value);
    }
}
