package com.campuscart.review.domain;

import com.campuscart.common.domain.BaseEntity;
import com.campuscart.order.domain.Order;
import com.campuscart.product.domain.Product;
import com.campuscart.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(name = "reviews",
        uniqueConstraints = @UniqueConstraint(name = "uq_reviews_reviewer_order_product",
                columnNames = {"reviewer_id", "order_id", "product_id"}),
        indexes = {
                @Index(name = "idx_reviews_product_status_created", columnList = "product_id,status,created_at"),
                @Index(name = "idx_reviews_reviewed_user_status", columnList = "reviewed_user_id,status"),
                @Index(name = "idx_reviews_order", columnList = "order_id")
        })
public class Review extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reviewer_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_reviews_reviewer"))
    private User reviewer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reviewed_user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_reviews_reviewed_user"))
    private User reviewedUser;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_reviews_product"))
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_reviews_order"))
    private Order order;

    @Column(name = "rating", nullable = false)
    private int rating;

    @Column(name = "review_text", nullable = false, length = 2000)
    private String reviewText;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReviewStatus status = ReviewStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moderated_by", foreignKey = @ForeignKey(name = "fk_reviews_moderator"))
    private User moderatedBy;

    @Column(name = "moderated_at")
    private Instant moderatedAt;

    protected Review() {
        // Required by JPA.
    }

    public Review(User reviewer, User reviewedUser, Product product, Order order,
                  int rating, String reviewText) {
        this.reviewer = reviewer;
        this.reviewedUser = reviewedUser;
        this.product = product;
        this.order = order;
        this.rating = rating;
        this.reviewText = reviewText;
    }

    public void moderate(User moderator, ReviewStatus status, Instant at) {
        this.moderatedBy = moderator;
        this.status = status;
        this.moderatedAt = at;
    }

    public User getReviewer() { return reviewer; }
    public User getReviewedUser() { return reviewedUser; }
    public Product getProduct() { return product; }
    public Order getOrder() { return order; }
    public int getRating() { return rating; }
    public String getReviewText() { return reviewText; }
    public ReviewStatus getStatus() { return status; }
    public User getModeratedBy() { return moderatedBy; }
    public Instant getModeratedAt() { return moderatedAt; }
}
