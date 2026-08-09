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
    private User otherStudent;
    private User remoteStudent;
    private User communitySeller;
    private Category category;
    private String studentToken;
    private String otherStudentToken;
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
        otherStudent = new User("other-" + suffix + "@other.edu", "Other Student", otherCollege);
        remoteStudent = new User("remote-" + suffix + "@remote.edu", "Remote Student", remoteCollege);
        communitySeller = User.community("community-" + suffix + "@example.com", "Community Seller", city, "+9198765" + suffix.replace("-", "").substring(0, 5));
        studentSeller.activateAfterEmailVerification();
        otherStudent.activateAfterEmailVerification();
        remoteStudent.activateAfterEmailVerification();
        communitySeller.activateAfterPhoneVerification();
        entityManager.persist(studentSeller);
        entityManager.persist(otherStudent);
        entityManager.persist(remoteStudent);
        entityManager.persist(communitySeller);
        entityManager.flush();

        studentToken = jwtService.generateAccessToken(studentSeller);
        otherStudentToken = jwtService.generateAccessToken(otherStudent);
        remoteStudentToken = jwtService.generateAccessToken(remoteStudent);
        communityToken = jwtService.generateAccessToken(communitySeller);
    }

    @Test
    @Transactional
    void supportsStudentAndCommunityProductTypes() throws Exception {
        JsonNode studentNew = create(studentToken, new CreateProductRequest(category.getId(), "Student New",
                "New calculator", new BigDecimal("20.00"), ProductType.NEW, SellingReach.MY_CAMPUS, 2));
        JsonNode studentSecondHand = create(studentToken, new CreateProductRequest(category.getId(), "Student Used",
                "Used textbook", new BigDecimal("10.00"), ProductType.SECOND_HAND, SellingReach.OTHER_COLLEGES, 1));
        JsonNode communityNew = create(communityToken, new CreateProductRequest(category.getId(), "Community New",
                "New lamp", new BigDecimal("15.00"), ProductType.NEW, SellingReach.PUBLIC, 1));
        JsonNode communitySecondHand = create(communityToken, new CreateProductRequest(category.getId(), "Community Used",
                "Used chair", new BigDecimal("8.00"), ProductType.SECOND_HAND, SellingReach.OTHER_COLLEGES, 1));

        assertThat(studentNew.get("data").get("productType").asText()).isEqualTo("NEW");
        assertThat(studentSecondHand.get("data").get("productType").asText()).isEqualTo("SECOND_HAND");
        assertThat(communityNew.get("data").get("productType").asText()).isEqualTo("NEW");
        assertThat(communitySecondHand.get("data").get("productType").asText()).isEqualTo("SECOND_HAND");
        assertThat(studentNew.get("data").get("sellingReach").asText()).isEqualTo("MY_CAMPUS");
        assertThat(studentSecondHand.get("data").get("sellingReach").asText()).isEqualTo("OTHER_COLLEGES");
        assertThat(communityNew.get("data").get("sellingReach").asText()).isEqualTo("PUBLIC");
    }

    @Test
    @Transactional
    void serverEnforcesVisibilityForEachMarketplaceScope() throws Exception {
        JsonNode campus = create(studentToken, product("Campus", ProductType.NEW, SellingReach.MY_CAMPUS));
        JsonNode otherColleges = create(studentToken, product("Nearby", ProductType.SECOND_HAND, SellingReach.OTHER_COLLEGES));
        JsonNode publicProduct = create(studentToken, product("Public", ProductType.NEW, SellingReach.PUBLIC));

        mockMvc.perform(get("/api/v1/products").param("scope", "MY_COLLEGE")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(3));

        mockMvc.perform(get("/api/v1/products").param("scope", "NEARBY_COLLEGES")
                        .header("Authorization", "Bearer " + otherStudentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2));

        mockMvc.perform(get("/api/v1/products").param("scope", "ALL_PRODUCTS")
                        .header("Authorization", "Bearer " + remoteStudentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1));

        mockMvc.perform(get("/api/v1/products/{id}", campus.get("data").get("id").asText())
                        .header("Authorization", "Bearer " + remoteStudentToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/products/{id}", publicProduct.get("data").get("id").asText())
                        .header("Authorization", "Bearer " + remoteStudentToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/products/{id}", otherColleges.get("data").get("id").asText())
                        .header("Authorization", "Bearer " + otherStudentToken))
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void onlySellerCanModifyOrDeleteProduct() throws Exception {
        JsonNode product = create(studentToken, product("Owned", ProductType.NEW, SellingReach.PUBLIC));
        String productId = product.get("data").get("id").asText();

        mockMvc.perform(patch("/api/v1/products/{id}", productId)
                        .header("Authorization", "Bearer " + otherStudentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new UpdateProductRequest(
                                null, "Hijacked", null, null, null, null, null))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value(ErrorCode.ACCESS_DENIED.name()));

        mockMvc.perform(delete("/api/v1/products/{id}", productId)
                        .header("Authorization", "Bearer " + otherStudentToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/v1/products/{id}", productId)
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new UpdateProductRequest(
                                null, "Updated", null, null, null, null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Updated"));

        mockMvc.perform(delete("/api/v1/products/{id}", productId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DELETED"));
    }

    @Test
    @Transactional
    void productSearchSupportsFiltersPaginationAndSorting() throws Exception {
        create(studentToken, product("Alpha Book", ProductType.NEW, SellingReach.PUBLIC));
        create(studentToken, new CreateProductRequest(category.getId(), "Beta Book", "Second hand",
                new BigDecimal("5.00"), ProductType.SECOND_HAND, SellingReach.PUBLIC, 1));

        mockMvc.perform(get("/api/v1/products")
                        .param("keyword", "alpha")
                        .param("productType", "NEW")
                        .param("minPrice", "10")
                        .param("page", "0")
                        .param("size", "1")
                        .param("sort", "price,asc")
                        .header("Authorization", "Bearer " + otherStudentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("Alpha Book"))
                .andExpect(jsonPath("$.data.size").value(1));
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
