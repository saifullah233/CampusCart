package com.campuscart.wishlist.domain;

import com.campuscart.common.domain.BaseEntity;
import com.campuscart.product.domain.Product;
import com.campuscart.user.domain.User;
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
        name = "wishlist_items",
        uniqueConstraints = @UniqueConstraint(name = "uq_wishlist_user_product", columnNames = {"user_id", "product_id"}),
        indexes = @Index(name = "idx_wishlist_items_user_created", columnList = "user_id,created_at"))
public class WishlistItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_wishlist_items_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, foreignKey = @ForeignKey(name = "fk_wishlist_items_product"))
    private Product product;

    protected WishlistItem() {
        // Required by JPA.
    }

    public WishlistItem(User user, Product product) {
        this.user = user;
        this.product = product;
    }

    public User getUser() { return user; }
    public Product getProduct() { return product; }
}
