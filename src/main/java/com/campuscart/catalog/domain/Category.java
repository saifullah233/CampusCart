package com.campuscart.catalog.domain;

import com.campuscart.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * A product category (flat taxonomy for Part 2).
 *
 * <p>Identified to clients by a URL-safe {@code slug} and shown by {@code name}; both
 * are unique. Hierarchy (a nullable parent reference) can be introduced later if the
 * product catalog requires sub-categories — it is intentionally omitted now.</p>
 */
@Entity
@Table(
        name = "categories",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_categories_slug", columnNames = "slug"),
                @UniqueConstraint(name = "uq_categories_name", columnNames = "name")
        })
public class Category extends BaseEntity {

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "slug", nullable = false, length = 140)
    private String slug;

    protected Category() {
        // Required by JPA.
    }

    public Category(String name, String slug) {
        this.name = name;
        this.slug = slug;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }
}
