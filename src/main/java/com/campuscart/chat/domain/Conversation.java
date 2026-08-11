package com.campuscart.chat.domain;

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
import java.time.Instant;

@Entity
@Table(
        name = "conversations",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_conversations_participants_product",
                columnNames = {"buyer_id", "seller_id", "product_id"}),
        indexes = {
                @Index(name = "idx_conversations_buyer_updated", columnList = "buyer_id,updated_at"),
                @Index(name = "idx_conversations_seller_updated", columnList = "seller_id,updated_at")
        })
public class Conversation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buyer_id", nullable = false, foreignKey = @ForeignKey(name = "fk_conversations_buyer"))
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false, foreignKey = @ForeignKey(name = "fk_conversations_seller"))
    private User seller;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, foreignKey = @ForeignKey(name = "fk_conversations_product"))
    private Product product;

    private Instant lastMessageAt;

    protected Conversation() {
        // Required by JPA.
    }

    public Conversation(User buyer, User seller, Product product) {
        this.buyer = buyer;
        this.seller = seller;
        this.product = product;
    }

    public void recordMessage(Instant sentAt) {
        this.lastMessageAt = sentAt;
    }

    public User getBuyer() { return buyer; }
    public User getSeller() { return seller; }
    public Product getProduct() { return product; }
    public Instant getLastMessageAt() { return lastMessageAt; }
}
