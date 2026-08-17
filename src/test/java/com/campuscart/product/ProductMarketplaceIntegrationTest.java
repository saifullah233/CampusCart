package com.campuscart.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campuscart.catalog.domain.Category;
import com.campuscart.college.domain.College;
import com.campuscart.common.exception.ErrorCode;
import com.campuscart.location.domain.City;
import com.campuscart.product.domain.ProductType;
import com.campuscart.product.domain.SellingReach;
import com.campuscart.product.dto.CreateProductRequest;
import com.campuscart.product.dto.UpdateProductRequest;
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
class ProductMarketplaceIntegrationTest extends AbstractMySqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JwtService jwtService;

    private User studentSeller;
    private User sameCampusStudent;
    private User otherCampusStudent;
    private User remoteStudent;
    private User communityUser;
    private Category category;

    private String studentSellerToken;
    private String sameCampusStudentToken;
    private String otherCampusStudentToken;
    private String remoteStudentToken;
    private String communityToken;

    @BeforeEach
    void seedMarketplace() {
        String suffix = UUID.randomUUID().toString();
        City city = new City("Mumbai-" + suffix, "Maharashtra");
        City remoteCity = new City("Pune-" + suffix, "Maharashtra");
        entityManager.persist(city);
        entityManager.persist(remoteCity);

        College sellerCollege = new College("Seller College-" + suffix, city);
        College otherCollege = new College("Other College-" + suffix, city);
        College remoteCollege = new College("Remote College-" + suffix, remoteCity);
        entityManager.persist(sellerCollege);
        entityManager.persist(otherCollege);
        entityManager.persist(remoteCollege);

        category = new Category("Books-" + suffix, "books-" + suffix);
        entityManager.persist(category);

        studentSeller = new User("seller-" + suffix + "@seller.edu", "Student Seller", sellerCollege);
        sameCampusStudent = new User("same-" + suffix + "@seller.edu", "Same Campus Student", sellerCollege);
        otherCampusStudent = new User("other-" + suffix + "@other.edu", "Other Student", otherCollege);
        remoteStudent = new User("remote-" + suffix + "@remote.edu", "Remote Student", remoteCollege);
        communityUser = User.community("community-" + suffix + "@example.com", "Community User", city, "+9198765" + suffix.replace("-", "").substring(0, 5));

        studentSeller.activateAfterEmailVerification();
        sameCampusStudent.activateAfterEmailVerification();
        otherCampusStudent.activateAfterEmailVerification();
        remoteStudent.activateAfterEmailVerification();
        communityUser.activateAfterEmailVerification();

        entityManager.persist(studentSeller);
        entityManager.persist(sameCampusStudent);
        entityManager.persist(otherCampusStudent);
        entityManager.persist(remoteStudent);
        entityManager.persist(communityUser);
        entityManager.flush();

        studentSellerToken = jwtService.generateAccessToken(studentSeller);
        sameCampusStudentToken = jwtService.generateAccessToken(sameCampusStudent);
        otherCampusStudentToken = jwtService.generateAccessToken(otherCampusStudent);
        remoteStudentToken = jwtService.generateAccessToken(remoteStudent);
        communityToken = jwtService.generateAccessToken(communityUser);
    }

    @Test
    @Transactional
    void studentAndCommunityListingCreationRules() throws Exception {
        // 10. Student creates CAMPUS_ONLY listing -> ALLOWED
        JsonNode campusListing = create(studentSellerToken, product("Campus Calc", ProductType.NEW, SellingReach.CAMPUS_ONLY));
        assertThat(campusListing.get("data").get("sellingReach").asText()).isEqualTo("CAMPUS_ONLY");

        // 11. Student creates OUTSIDE_CAMPUS listing -> ALLOWED
        JsonNode outsideListing = create(studentSellerToken, product("Outside Book", ProductType.SECOND_HAND, SellingReach.OUTSIDE_CAMPUS));
        assertThat(outsideListing.get("data").get("sellingReach").asText()).isEqualTo("OUTSIDE_CAMPUS");

        // 9. Community creates OUTSIDE_CAMPUS listing -> ALLOWED
        JsonNode communityOutside = create(communityToken, product("Community Item", ProductType.NEW, SellingReach.OUTSIDE_CAMPUS));
        assertThat(communityOutside.get("data").get("sellingReach").asText()).isEqualTo("OUTSIDE_CAMPUS");

        // 8. Community attempts to create CAMPUS_ONLY listing -> DENIED
        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + communityToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(product("Forbidden Campus", ProductType.NEW, SellingReach.CAMPUS_ONLY))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value(ErrorCode.BUSINESS_RULE_VIOLATION.name()));
    }

    @Test
    @Transactional
    void campusOnlyVisibilityEnforcedAcrossDirectGetAndSearch() throws Exception {
        JsonNode campusProduct = create(studentSellerToken, product("Campus Only Item", ProductType.NEW, SellingReach.CAMPUS_ONLY));
        String productId = campusProduct.get("data").get("id").asText();

        // 1. Student A views their own campus listing -> ALLOWED
        mockMvc.perform(get("/api/v1/products/{id}", productId)
                        .header("Authorization", "Bearer " + studentSellerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Campus Only Item"));

        // 2. Student B from same campus views CAMPUS_ONLY listing -> ALLOWED
        mockMvc.perform(get("/api/v1/products/{id}", productId)
                        .header("Authorization", "Bearer " + sameCampusStudentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Campus Only Item"));

        // 3. Student from different campus views CAMPUS_ONLY listing -> DENIED / 404
        mockMvc.perform(get("/api/v1/products/{id}", productId)
                        .header("Authorization", "Bearer " + otherCampusStudentToken))
                .andExpect(status().isNotFound());

        // 4 & 5. Community user attempts direct GET by product ID for CAMPUS_ONLY -> DENIED / 404
        mockMvc.perform(get("/api/v1/products/{id}", productId)
                        .header("Authorization", "Bearer " + communityToken))
                .andExpect(status().isNotFound());

        // 12. Search/list endpoint cannot leak CAMPUS_ONLY products to community users
        mockMvc.perform(get("/api/v1/products")
                        .header("Authorization", "Bearer " + communityToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(0));

        // Same campus student discovers it in list
        mockMvc.perform(get("/api/v1/products")
                        .header("Authorization", "Bearer " + sameCampusStudentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1));

        // Different campus student does NOT discover it
        mockMvc.perform(get("/api/v1/products")
                        .header("Authorization", "Bearer " + otherCampusStudentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(0));
    }

    @Test
    @Transactional
    void outsideCampusVisibilityAccessibleToBothStudentAndCommunity() throws Exception {
        JsonNode outsideProduct = create(studentSellerToken, product("Outside Campus Item", ProductType.NEW, SellingReach.OUTSIDE_CAMPUS));
        String productId = outsideProduct.get("data").get("id").asText();

        // 6. Student views OUTSIDE_CAMPUS listing -> ALLOWED
        mockMvc.perform(get("/api/v1/products/{id}", productId)
                        .header("Authorization", "Bearer " + remoteStudentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Outside Campus Item"));

        // 7. Community views OUTSIDE_CAMPUS listing -> ALLOWED
        mockMvc.perform(get("/api/v1/products/{id}", productId)
                        .header("Authorization", "Bearer " + communityToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Outside Campus Item"));

        // Both find it in marketplace search
        mockMvc.perform(get("/api/v1/products")
                        .header("Authorization", "Bearer " + remoteStudentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1));

        mockMvc.perform(get("/api/v1/products")
                        .header("Authorization", "Bearer " + communityToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1));
    }

    @Test
    @Transactional
    void paginationAndFilteringCannotBypassVisibilityRules() throws Exception {
        // Create 3 CAMPUS_ONLY products and 2 OUTSIDE_CAMPUS products
        create(studentSellerToken, product("Campus Item 1", ProductType.NEW, SellingReach.CAMPUS_ONLY));
        create(studentSellerToken, product("Campus Item 2", ProductType.NEW, SellingReach.CAMPUS_ONLY));
        create(studentSellerToken, product("Campus Item 3", ProductType.NEW, SellingReach.CAMPUS_ONLY));
        create(studentSellerToken, product("Outside Item 1", ProductType.NEW, SellingReach.OUTSIDE_CAMPUS));
        create(studentSellerToken, product("Outside Item 2", ProductType.NEW, SellingReach.OUTSIDE_CAMPUS));

        // 13. Pagination for Community user: Only sees the 2 OUTSIDE_CAMPUS products across pages
        mockMvc.perform(get("/api/v1/products")
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", "Bearer " + communityToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content.length()").value(2));

        // 14. Sorting and keyword filters for Community: querying for "Campus" yields 0 results
        mockMvc.perform(get("/api/v1/products")
                        .param("keyword", "Campus")
                        .param("sort", "price,asc")
                        .header("Authorization", "Bearer " + communityToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0))
                .andExpect(jsonPath("$.data.content.length()").value(0));

        // Same query for same campus student returns the 3 CAMPUS_ONLY items
        mockMvc.perform(get("/api/v1/products")
                        .param("keyword", "Campus")
                        .header("Authorization", "Bearer " + sameCampusStudentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.content.length()").value(3));
    }

    @Test
    @Transactional
    void onlySellerCanModifyOrDeleteProduct() throws Exception {
        JsonNode product = create(studentSellerToken, product("Owned", ProductType.NEW, SellingReach.OUTSIDE_CAMPUS));
        String productId = product.get("data").get("id").asText();

        mockMvc.perform(patch("/api/v1/products/{id}", productId)
                        .header("Authorization", "Bearer " + otherCampusStudentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new UpdateProductRequest(
                                null, "Hijacked", null, null, null, null, null))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value(ErrorCode.ACCESS_DENIED.name()));

        mockMvc.perform(delete("/api/v1/products/{id}", productId)
                        .header("Authorization", "Bearer " + otherCampusStudentToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/v1/products/{id}", productId)
                        .header("Authorization", "Bearer " + studentSellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new UpdateProductRequest(
                                null, "Updated", null, null, null, null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Updated"));

        mockMvc.perform(delete("/api/v1/products/{id}", productId)
                        .header("Authorization", "Bearer " + studentSellerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DELETED"));
    }

    private CreateProductRequest product(String title, ProductType type, SellingReach reach) {
        return new CreateProductRequest(category.getId(), title, "Description", new BigDecimal("12.50"), type, reach, 1);
    }

    private JsonNode create(String token, CreateProductRequest request) throws Exception {
        String body = mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }
}
