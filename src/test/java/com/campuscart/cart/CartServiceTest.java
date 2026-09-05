package com.campuscart.cart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.campuscart.cart.domain.CartItem;
import com.campuscart.cart.dto.CartItemResponse;
import com.campuscart.cart.dto.CartResponse;
import com.campuscart.cart.repository.CartItemRepository;
import com.campuscart.cart.service.CartService;
import com.campuscart.catalog.domain.Category;
import com.campuscart.college.domain.College;
import com.campuscart.location.domain.City;
import com.campuscart.product.domain.Product;
import com.campuscart.product.domain.ProductType;
import com.campuscart.product.domain.SellingReach;
import com.campuscart.product.repository.ProductImageRepository;
import com.campuscart.product.repository.ProductRepository;
import com.campuscart.product.service.ProductService;
import com.campuscart.user.domain.User;
import com.campuscart.user.service.UserService;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartItemRepository cartRepository;

    @Mock
    private ProductService productService;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private UserService userService;

    private CartService cartService;

    private User buyer;
    private User seller;
    private College college;
    private City city;
    private Category category;

    @BeforeEach
    void setUp() {
        cartService = new CartService(cartRepository, productService, productRepository, productImageRepository, userService);

        city = new City("Pune", "Maharashtra");
        college = new College("COEP", city);
        buyer = new User("buyer@campuscart.edu", "Buyer Student", college);
        seller = new User("seller@campuscart.edu", "Seller Student", college);
        ReflectionTestUtils.setField(buyer, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(seller, "id", UUID.randomUUID());
        category = new Category("Electronics", "electronics");
    }

    @Test
    void getReturnsCorrectSubtotalAndLineTotalsForActiveCartItems() {
        Product p1 = new Product(seller, college, city, category, "Calculus Textbook", "Good condition",
                new BigDecimal("50.00"), ProductType.SECOND_HAND, SellingReach.CAMPUS_ONLY, 5);
        ReflectionTestUtils.setField(p1, "id", UUID.randomUUID());

        Product p2 = new Product(seller, college, city, category, "Lab Coat", "Clean",
                new BigDecimal("150.00"), ProductType.SECOND_HAND, SellingReach.CAMPUS_ONLY, 3);
        ReflectionTestUtils.setField(p2, "id", UUID.randomUUID());

        CartItem item1 = new CartItem(buyer, p1, 2); // 2 * 50 = 100
        CartItem item2 = new CartItem(buyer, p2, 1); // 1 * 150 = 150

        when(userService.requireActive(buyer.getId())).thenReturn(buyer);
        when(cartRepository.findByUserIdOrderByCreatedAtAsc(buyer.getId())).thenReturn(List.of(item1, item2));
        when(productImageRepository.findByProductIdInOrderByProductIdAscCreatedAtAsc(any())).thenReturn(Collections.emptyList());

        CartResponse response = cartService.get(buyer.getId());

        assertThat(response.items()).hasSize(2);

        CartItemResponse respItem1 = response.items().get(0);
        assertThat(respItem1.unitPrice()).isEqualByComparingTo("50.00");
        assertThat(respItem1.quantity()).isEqualTo(2);
        assertThat(respItem1.lineTotal()).isEqualByComparingTo("100.00");
        assertThat(respItem1.available()).isTrue();

        CartItemResponse respItem2 = response.items().get(1);
        assertThat(respItem2.unitPrice()).isEqualByComparingTo("150.00");
        assertThat(respItem2.quantity()).isEqualTo(1);
        assertThat(respItem2.lineTotal()).isEqualByComparingTo("150.00");
        assertThat(respItem2.available()).isTrue();

        // Subtotal / Total should be 100 + 150 = 250.00
        assertThat(response.total()).isEqualByComparingTo("250.00");
        assertThat(response.totalAmount()).isEqualByComparingTo("250.00");
        assertThat(response.checkoutAvailable()).isTrue();
        assertThat(response.checkoutReady()).isTrue();
    }

    @Test
    void getHandlesEmptyCartSafely() {
        when(userService.requireActive(buyer.getId())).thenReturn(buyer);
        when(cartRepository.findByUserIdOrderByCreatedAtAsc(buyer.getId())).thenReturn(Collections.emptyList());

        CartResponse response = cartService.get(buyer.getId());

        assertThat(response.items()).isEmpty();
        assertThat(response.total()).isEqualByComparingTo("0");
        assertThat(response.totalAmount()).isEqualByComparingTo("0");
        assertThat(response.checkoutAvailable()).isFalse();
        assertThat(response.checkoutReady()).isFalse();
    }

    @Test
    void getExcludesUnavailableItemsFromTotal() {
        Product p1 = new Product(seller, college, city, category, "Available Product", "Desc",
                new BigDecimal("75.00"), ProductType.NEW, SellingReach.CAMPUS_ONLY, 5);
        ReflectionTestUtils.setField(p1, "id", UUID.randomUUID());

        Product p2Out = new Product(seller, college, city, category, "Out of Stock Product", "Desc",
                new BigDecimal("120.00"), ProductType.NEW, SellingReach.CAMPUS_ONLY, 0);
        ReflectionTestUtils.setField(p2Out, "id", UUID.randomUUID());

        CartItem item1 = new CartItem(buyer, p1, 1); // 1 * 75 = 75
        CartItem item2 = new CartItem(buyer, p2Out, 2); // unavailable (qty 2 > stock 0)

        when(userService.requireActive(buyer.getId())).thenReturn(buyer);
        when(cartRepository.findByUserIdOrderByCreatedAtAsc(buyer.getId())).thenReturn(List.of(item1, item2));
        when(productImageRepository.findByProductIdInOrderByProductIdAscCreatedAtAsc(any())).thenReturn(Collections.emptyList());

        CartResponse response = cartService.get(buyer.getId());

        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).available()).isTrue();
        assertThat(response.items().get(1).available()).isFalse();

        // Total should only count available items = 75.00
        assertThat(response.total()).isEqualByComparingTo("75.00");
        assertThat(response.totalAmount()).isEqualByComparingTo("75.00");
        assertThat(response.checkoutAvailable()).isFalse();
        assertThat(response.checkoutReady()).isFalse();
    }
}
