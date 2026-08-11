package com.campuscart.product.domain;

import com.campuscart.common.domain.BaseEntity;
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
        name = "product_likes",
        uniqueConstraints = @UniqueConstraint(name = "uq_product_likes_user_product", columnNames = {"user_id", "product_id"}),
        indexes = @Index(name = "idx_product_likes_product_created", columnList = "product_id,created_at"))
public class ProductLike extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_product_likes_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, foreignKey = @ForeignKey(name = "fk_product_likes_product"))
    private Product product;

    protected ProductLike() {
        // Required by JPA.
    }

    public ProductLike(User user, Product product) {
        this.user = user;
        this.product = product;
    }

    public User getUser() { return user; }
    public Product getProduct() { return product; }
}
