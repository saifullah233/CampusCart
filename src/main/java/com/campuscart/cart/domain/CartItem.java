package com.campuscart.cart.domain;

import com.campuscart.common.domain.BaseEntity;
import com.campuscart.product.domain.Product;
import com.campuscart.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "cart_items",
        uniqueConstraints = @UniqueConstraint(name = "uq_cart_user_product", columnNames = {"user_id", "product_id"}),
        indexes = @Index(name = "idx_cart_items_user_created", columnList = "user_id,created_at"))
public class CartItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_cart_items_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, foreignKey = @ForeignKey(name = "fk_cart_items_product"))
    private Product product;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    protected CartItem() {
        // Required by JPA.
    }

    public CartItem(User user, Product product, int quantity) {
        this.user = user;
        this.product = product;
        this.quantity = quantity;
    }

    public void changeQuantity(int quantity) { this.quantity = quantity; }
    public User getUser() { return user; }
    public Product getProduct() { return product; }
    public int getQuantity() { return quantity; }
}
