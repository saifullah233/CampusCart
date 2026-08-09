package com.campuscart.order.domain;

import com.campuscart.common.domain.BaseEntity;
import com.campuscart.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "orders")
public class Order extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buyer_id", nullable = false, foreignKey = @ForeignKey(name = "fk_orders_buyer"))
    private User buyer;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status = OrderStatus.PLACED;

    protected Order() {
        // Required by JPA.
    }

    public Order(User buyer, BigDecimal totalAmount) {
        this.buyer = buyer;
        this.totalAmount = totalAmount;
    }

    public void transitionTo(OrderStatus target) {
        if (!isAllowedTransition(status, target)) {
            throw new IllegalStateException("Order cannot transition from " + status + " to " + target + ".");
        }
        this.status = target;
    }

    private boolean isAllowedTransition(OrderStatus current, OrderStatus target) {
        return switch (current) {
            case PLACED -> target == OrderStatus.ACCEPTED
                    || target == OrderStatus.REJECTED || target == OrderStatus.CANCELLED;
            case ACCEPTED -> target == OrderStatus.SHIPPED || target == OrderStatus.CANCELLED;
            case SHIPPED -> target == OrderStatus.DELIVERED;
            case DELIVERED -> target == OrderStatus.COMPLETED;
            case REJECTED, COMPLETED, CANCELLED -> false;
        };
    }

    public User getBuyer() { return buyer; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public OrderStatus getStatus() { return status; }
}
