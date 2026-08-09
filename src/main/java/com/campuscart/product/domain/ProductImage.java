package com.campuscart.product.domain;

import com.campuscart.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(
        name = "product_images",
        indexes = @Index(name = "idx_product_images_product_created", columnList = "product_id,created_at"))
public class ProductImage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_product_images_product"))
    private Product product;

    @Column(name = "storage_key", nullable = false, unique = true, length = 512)
    private String storageKey;

    @Column(name = "delivery_url", nullable = false, length = 1024)
    private String deliveryUrl;

    @Column(name = "content_type", nullable = false, length = 80)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    protected ProductImage() {
        // Required by JPA.
    }

    public ProductImage(Product product, String storageKey, String deliveryUrl,
                        String contentType, long sizeBytes) {
        this.product = product;
        this.storageKey = storageKey;
        this.deliveryUrl = deliveryUrl;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
    }

    public Product getProduct() { return product; }
    public String getStorageKey() { return storageKey; }
    public String getDeliveryUrl() { return deliveryUrl; }
    public String getContentType() { return contentType; }
    public long getSizeBytes() { return sizeBytes; }
}
