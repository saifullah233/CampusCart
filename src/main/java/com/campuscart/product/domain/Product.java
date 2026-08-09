package com.campuscart.product.domain;

import com.campuscart.catalog.domain.Category;
import com.campuscart.college.domain.College;
import com.campuscart.common.domain.BaseEntity;
import com.campuscart.location.domain.City;
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
import java.math.BigDecimal;

@Entity
@Table(
        name = "products",
        indexes = {
                @Index(name = "idx_products_status_reach_city_created", columnList = "status,selling_reach,city_id,created_at"),
                @Index(name = "idx_products_status_reach_college_created", columnList = "status,selling_reach,college_id,created_at"),
                @Index(name = "idx_products_category_status_created", columnList = "category_id,status,created_at"),
                @Index(name = "idx_products_type_status_created", columnList = "product_type,status,created_at"),
                @Index(name = "idx_products_price_status_created", columnList = "price,status,created_at"),
                @Index(name = "idx_products_seller_status_created", columnList = "seller_id,status,created_at")
        })
public class Product extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false, foreignKey = @ForeignKey(name = "fk_products_seller"))
    private User seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "college_id", foreignKey = @ForeignKey(name = "fk_products_college"))
    private College college;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "city_id", nullable = false, foreignKey = @ForeignKey(name = "fk_products_city"))
    private City city;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false, foreignKey = @ForeignKey(name = "fk_products_category"))
    private Category category;

    @Column(name = "title", nullable = false, length = 180)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false, length = 20)
    private ProductType productType;

    @Enumerated(EnumType.STRING)
    @Column(name = "selling_reach", nullable = false, length = 30)
    private SellingReach sellingReach;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProductStatus status = ProductStatus.ACTIVE;

    protected Product() {
        // Required by JPA.
    }

    public Product(User seller, College college, City city, Category category, String title,
                   String description, BigDecimal price, ProductType productType,
                   SellingReach sellingReach, int quantity) {
        this.seller = seller;
        this.college = college;
        this.city = city;
        this.category = category;
        this.title = title;
        this.description = description;
        this.price = price;
        this.productType = productType;
        this.sellingReach = sellingReach;
        this.quantity = quantity;
    }

    public void updateDetails(Category category, String title, String description, BigDecimal price,
                              ProductType productType, SellingReach sellingReach, int quantity) {
        this.category = category;
        this.title = title;
        this.description = description;
        this.price = price;
        this.productType = productType;
        this.sellingReach = sellingReach;
        this.quantity = quantity;
    }

    public void markSold() {
        this.quantity = 0;
        this.status = ProductStatus.SOLD;
    }

    public void reserveQuantity(int requestedQuantity) {
        if (requestedQuantity < 1 || status != ProductStatus.ACTIVE || quantity < requestedQuantity) {
            throw new IllegalStateException("Product is unavailable for the requested quantity.");
        }
        quantity -= requestedQuantity;
        if (quantity == 0) {
            status = ProductStatus.SOLD;
        }
    }

    public void restoreQuantity(int restoredQuantity) {
        if (restoredQuantity < 1) {
            throw new IllegalArgumentException("Restored quantity must be positive.");
        }
        quantity += restoredQuantity;
        if (status == ProductStatus.SOLD) {
            status = ProductStatus.ACTIVE;
        }
    }

    public void activate() {
        if (status == ProductStatus.DELETED || status == ProductStatus.SOLD) {
            throw new IllegalStateException("Deleted or sold products cannot be activated.");
        }
        this.status = ProductStatus.ACTIVE;
    }

    public void deactivate() {
        if (status == ProductStatus.DELETED || status == ProductStatus.SOLD) {
            throw new IllegalStateException("Deleted or sold products cannot be deactivated.");
        }
        this.status = ProductStatus.INACTIVE;
    }

    public void softDelete() {
        this.status = ProductStatus.DELETED;
    }

    public User getSeller() { return seller; }
    public College getCollege() { return college; }
    public City getCity() { return city; }
    public Category getCategory() { return category; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public ProductType getProductType() { return productType; }
    public SellingReach getSellingReach() { return sellingReach; }
    public int getQuantity() { return quantity; }
    public ProductStatus getStatus() { return status; }
}
