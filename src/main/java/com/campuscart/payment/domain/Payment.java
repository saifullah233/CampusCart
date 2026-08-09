package com.campuscart.payment.domain;

import com.campuscart.common.domain.BaseEntity;
import com.campuscart.order.domain.Order;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "payments")
public class Payment extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true,
            foreignKey = @ForeignKey(name = "fk_payments_order"))
    private Order order;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PaymentStatus status = PaymentStatus.NOT_CONNECTED;

    @Column(name = "provider", length = 80)
    private String provider;

    @Column(name = "provider_payment_id", length = 180)
    private String providerPaymentId;

    protected Payment() {
        // Required by JPA.
    }

    public Payment(Order order, BigDecimal amount) {
        this.order = order;
        this.amount = amount;
    }

    public void recordProviderInitialization(String provider, String providerPaymentId, PaymentStatus status) {
        this.provider = provider;
        this.providerPaymentId = providerPaymentId;
        this.status = status;
    }

    public Order getOrder() { return order; }
    public BigDecimal getAmount() { return amount; }
    public PaymentStatus getStatus() { return status; }
    public String getProvider() { return provider; }
    public String getProviderPaymentId() { return providerPaymentId; }
}
